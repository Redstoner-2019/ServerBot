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

            userPrompt = "Can you analyze the following song lyrics? [Spoken]\n" +
                    "\"And then I found out how hard it is to really change\n" +
                    "Even Hell can get comfy once you've settled in\n" +
                    "I just wanted the numb inside me to leave\n" +
                    "No matter how fucked you get\n" +
                    "Sorrow is there when you come back down\n" +
                    "Funny thing is, all I ever wanted I already had\n" +
                    "There's glimpses of heaven in every day\n" +
                    "In the friends I have, the music I make, with the love I feel\n" +
                    "I just had to start again\"\n" +
                    "\n" +
                    "[Verse 1]\n" +
                    "The days are a deathwish\n" +
                    "A witch-hunt for an exit\n" +
                    "I am powerless\n" +
                    "The fragile, the broken\n" +
                    "Sit in circles and stay unspoken\n" +
                    "We are powerless\n" +
                    "Because we all walk alone on an empty staircase\n" +
                    "Silent halls and nameless faces\n" +
                    "I am powerless\n" +
                    "\n" +
                    "[Pre-Chorus]\n" +
                    "Everybody wants to go to Heaven, but nobody wants to die\n" +
                    "I can't fear death, no longer, I've died a thousand times\n" +
                    "Why explore the universe when we don't know ourselves?\n" +
                    "There's an emptiness inside our heads that no one dares to dwell\n" +
                    "See rock shows near Brooklyn\n" +
                    "Get tickets as low as $58\n" +
                    "You might also like\n" +
                    "loml\n" +
                    "Taylor Swift\n" +
                    "So Long, London\n" +
                    "Taylor Swift\n" +
                    "Guilty as Sin?\n" +
                    "Taylor Swift\n" +
                    "[Chorus]\n" +
                    "Throw me to the flames\n" +
                    "Watch me burn\n" +
                    "Set my world ablaze\n" +
                    "Watch me burn\n" +
                    "\n" +
                    "[Verse 2]\n" +
                    "How are we on a scale of one to ten?\n" +
                    "Could you tell me what you see?\n" +
                    "Do you wanna talk about it?\n" +
                    "How does that make you feel?\n" +
                    "Have you ever took a blade to your wrists?\n" +
                    "Have you been skipping meals?\n" +
                    "We're gonna try something new today\n" +
                    "How does that make you feel?\n" +
                    "\n" +
                    "[Bridge]\n" +
                    "Hold me close, don't let go, watch me (Burn)\n" +
                    "Hold me close, don't let go, watch me (Burn)\n" +
                    "Hold me close, don't let go, watch me (Burn)\n" +
                    "In this hospital for souls\n" +
                    "Hold me close, don't let go, watch me (Burn)\n" +
                    "Hold me close, don't let go, watch me (Burn)\n" +
                    "Hold me close, don't let go, watch me (Burn)\n" +
                    "In this hospital for souls\n" +
                    "Hold me close, don't let go, watch me (Burn)\n" +
                    "Hold me close, don't let go, watch me (Burn)\n" +
                    "Hold me close, don't let go, watch me (Burn)\n" +
                    "In this hospital for souls\n" +
                    "[Outro]\n" +
                    "Hold me close, don't let go, watch me burn\n" +
                    "Hold me close, don't let go, watch me burn\n" +
                    "Hold me close, don't let go, watch me burn\n" +
                    "In this hospital for souls\n" +
                    "Hold me close, don't let go, watch me burn\n" +
                    "Hold me close, don't let go, watch me burn\n" +
                    "Hold me close, don't let go, watch me burn\n" +
                    "In this hospital for souls";


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
