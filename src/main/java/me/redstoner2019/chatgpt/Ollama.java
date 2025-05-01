package me.redstoner2019.chatgpt;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;

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

        if (jsonResponse.has("response")) {
            return jsonResponse.getString("response");
        } else {
            System.out.println("Warning: Ollama returned unexpected data: " + jsonResponse.toString(2));
            return "Error";
        }
    }


    public static void main(String[] args) {
        try {
            String modelName = "mistral";
            String userPrompt = "You are a content moderation assistant.  \n" +
                    "You will be given a prompt intended for AI image generation.  \n" +
                    "Your task is to return a **comma-separated list of content flags** that describe any **explicit, dangerous, abusive, or illegal content** detected in the prompt.  \n" +
                    "\n" +
                    "Only respond with these flags if they apply (use exact capitalization):  \n" +
                    "CHILDREN, NUDITY, SEXUAL, VIOLENCE, GORE, INCEST, RAPE, FETISH, BDSM, ABUSE, BESTIALITY\n" +
                    "\n" +
                    "If none apply, return `NONE`.\n" +
                    "\n" +
                    "Do NOT explain your answer. Do not add commentary.\n" +
                    "\n" +
                    "Prompt:  \n" + "multiple girls, group of girls,2girls, 3girls, group of girls, friends, duo, trio , Awakening Desires, Anime-inspired Masterpiece, Best Quality, Horny Odyssey, Backdrop, Fantastical world, Vibrant colors, Enchanting landscapes, Single girl, Radiant, Standing tall, Detailing, Hair, Strands, Meticulously crafted, Dynamic lighting, Emotions, Raw, Intentions, Attire, Naked, Empowered, Inhibitions, Rolls of eyes, Dominance, Control, Tongue, Glistens, Unspoken desires, Ambiance, Sexual content, Erotic, Intensity, Sperm stains, Tears of sweat, Props, Sex toys, Kinks, Preferences, Setting, Suggestive objects, Emotions, Seductive confidence, Passion, Raw emotion, Sensual, Provocative, Arousing, Coldest hearts, graphic, nude, sex toys ,extreme, raw, unapologetic, unforgiving, forced, nsfw, naked, horny, eye rolls, tongue, sperm, fluid, tears, sweat, posing, sex toys, kinks, sexual content, naked, 1girl, hardcore sex, Erotic, Intense, Passionate, Seductive, Explicit, Graphic, Bold, Sensual, Arousing, Provocative, Nipples";

            String result = askModel(modelName, userPrompt);
            System.out.println("Model said: " + result);
            //System.out.println();
            //System.out.println(result.replaceAll("\"","").replaceAll("'",""));

            //System.out.println("\"fantastical world\", \"vibrant colors\", \"enchanting landscapes\", \"single girl\", \"radiant\", \"horny\", \"colorful hair\", \"waterfall of lust\", \"detailed\", \"anime-inspired\", \"flawless anatomy\", \"curve and contour\", \"naked\", \"empowered\", \"rolls of eyes\", \"dominance and control\", \"tongue glistens\", \"unspoken desires\", \"sexual content\", \"erotic atmosphere\", \"intensity\", \"charged\", \"sperm stains\", \"tears of sweat\", \"sex toys\", \"kinks and preferences\", \"suggestive objects\", \"seductive confidence\", \"passion\", \"raw emotion\", \"sensual\", \"provocative\"".replaceAll("\"",""));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
