package com.llms.model;

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
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @PostMapping("/generate")
    public Map<String, String> generate(@RequestBody Map<String, String> body) {
        String prompt = body.get("prompt");

        Map<String, String> results = new HashMap<>();
        results.put("gemini", callGemini(prompt));
        results.put("groq", callGroq(prompt));

        return results;
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
            return "Groq error: " + e.getMessage();
        }
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
            return "Gemini error: " + e.getMessage();
        }
    }
}
