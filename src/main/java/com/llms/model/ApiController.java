package com.llms.model;

import com.llms.model.pojo.FallBackResponse;
import com.llms.model.pojo.ImageResponseBody;
import com.llms.model.pojo.PromptBody;
import com.llms.model.pojo.ResponseBody;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.llms.model.utils.Constants.*;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${together.api.key}")
    private String togetherApiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @PostMapping("/generate")
    public Object generate(@RequestBody PromptBody promptBody) {
        String prompt = promptBody.getPrompt();

        if(prompt != null && !prompt.isEmpty()) {
            ResponseBody responseBody = new ResponseBody();
            responseBody.setGemini(callGemini(prompt));
            responseBody.setGroq(callGroq(prompt));
            responseBody.setTogether(callTogetherAI(prompt));
            return responseBody;
        }
        else{
            FallBackResponse fallBackResponse = new FallBackResponse();
            fallBackResponse.setResponse("Give a proper Prompt Once in a while!");
            return fallBackResponse;
        }
    }

    @PostMapping("/generateImage")
    public Object generateImage(@RequestBody PromptBody promptBody){
        String prompt = promptBody.getPrompt();

        if(prompt != null && !prompt.isEmpty()) {
            ImageResponseBody responseBody = new ImageResponseBody();
            responseBody.setResponse(callTogetherApiForImage(prompt));
            return responseBody;
        }
        else{
            FallBackResponse fallBackResponse = new FallBackResponse();
            fallBackResponse.setResponse("Give a proper Prompt Once in a while!");
            return fallBackResponse;
        }

    }

    private String callGroq(String prompt) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .header("Authorization", "Bearer " + groqApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("""
                    {
                      "model": "llama3-70b-8192",
                      "messages": [{"role": "user", "content": "%s"}]
                    }
                """.formatted(prompt)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject json = new JSONObject(response.body());
            return json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

        } catch (Exception e) {
            return GROQ_ERROR_PREFIX + e.getMessage();
        }
    }

    private String callTogetherAI(String prompt){
        try {
            String API_URL = "https://api.together.xyz/v1/chat/completions";
            String model = "mistralai/Mixtral-8x7B-Instruct-v0.1";

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.7);
            requestBody.put("top_p", 0.9);
            requestBody.put("max_tokens", 256);

            JSONArray messages = new JSONArray();
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.put(userMessage);

            requestBody.put("messages", messages);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Authorization", "Bearer " + togetherApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String assistantReply = "";

            JSONArray responseArray = new JSONObject(response.body()).getJSONArray("choices");
            for(Object object : responseArray){
                JSONObject jsonObject = (JSONObject) object;
                assistantReply += jsonObject.getJSONObject("message").getString("content");
            }
//            String assistantReply = new JSONObject(response.body())
//                    .getJSONArray("choices")
//                    .getJSONObject(0)
//                    .getJSONObject("message")
//                    .getString("content");

            return assistantReply;
        }
        catch (Exception e) {
            return TOGETHER_ERROR_PREFIX + e.getMessage();
        }
    }

    private String callTogetherApiForImage(String prompt){
        try {
            String API_URL = "https://api.together.xyz/inference";
            String model = "black-forest-labs/FLUX.1-kontext-dev";

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);
            requestBody.put("prompt", prompt);
            requestBody.put("n", 4);
            requestBody.put("steps", 20);
            requestBody.put("width", 512);
            requestBody.put("height", 512);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Authorization", "Bearer " + togetherApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> res = HttpClient.newHttpClient()
                    .send(req, HttpResponse.BodyHandlers.ofString());

            JSONObject json = new JSONObject(res.body());
            String base64 = json.getJSONObject("output")
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getString("image_base64");

            return base64;
        }
        catch (Exception e) {
            return TOGETHER_ERROR_PREFIX + e.getMessage();
        }
    }

    private String getExceptionMessage(String error){
        return EXCEPTION_MSG + error;
    }

    private String callGemini(String prompt) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey;

            String requestBody = new JSONObject()
                    .put("contents", new JSONArray().put(
                            new JSONObject().put("parts", new JSONArray().put(
                                    new JSONObject().put("text", prompt)
                            ))
                    ))
                    .put("generationConfig", new JSONObject()
                            .put("maxOutputTokens", 100)
                    ).toString();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject json = new JSONObject(response.body());

            if (!json.has("candidates")) {
                return "Gemini error: No candidates returned.\nRaw response: " + response.body();
            }

            return json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");

        } catch (Exception e) {
            return GEMINI_ERROR_PREFIX + e.getMessage();
        }
    }
}
