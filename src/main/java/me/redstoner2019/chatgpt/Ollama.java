package me.redstoner2019.chatgpt;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Ollama {
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";

    public static String askModel(String modelName, String prompt) throws IOException, InterruptedException {
        JSONObject body = new JSONObject();
        body.put("model", modelName);
        body.put("prompt", prompt);
        body.put("stream", false);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject jsonResponse = new JSONObject(response.body());

        // ✨ SAFE check
        if (jsonResponse.has("response")) {
            return jsonResponse.getString("response");
        } else {
            System.out.println("Warning: Ollama returned unexpected data: " + jsonResponse.toString(2));
            return "Error"; // Fallback
        }
    }


    public static void main(String[] args) {
        try {
            String modelName = "tinyllama"; // or "phi", "mistral", etc.
            String userPrompt = "Answer like a meme expert: What is love?";

            String result = askModel(modelName, userPrompt);
            System.out.println("Model said: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
