package me.redstoner2019.chatgpt;

import me.redstoner2019.events.ChatEvent;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class StableDiffusion {

    private static HashMap<String, JSONObject> status = new HashMap<>();
    private static HashMap<String, BufferedImage> results = new HashMap<>();

    public static HashMap<String, JSONObject> presetNameMappings = new HashMap<>();
    public static HashMap<String, JSONObject> presetNameDisplayMappings = new HashMap<>();
    public static HashMap<String, String> modelMappings = new HashMap<>();
    public static String staticIP = "127.0.0.1";

    static {
        reload();
        //staticIP = "redstonerdev.io";
    }

    /**
     * C:\Users\Redstoner_2019\Projects\StableDiffusion
     * cd Projects/StableDiffusion/stable-diffusion-webui
     * python -m venv venv
     * venv\Scripts\activate
     * python launch.py --api
     */

    public static String schedule(String model, String positivePrompt, String negativePrompt, String preset, int width, int height, int seed){
        String id = UUID.randomUUID().toString();
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("model", model);
        obj.put("positivePrompt", positivePrompt);
        obj.put("negativePrompt", negativePrompt);
        obj.put("positivePromptPreset", preset);
        obj.put("status", "In Queue");
        obj.put("width", width);
        obj.put("height", height);
        obj.put("seed", seed);

        int spot = 0;

        for(String s : status.keySet()){
            if(status.get(s).getInt("queueSpot") >= 0) spot++;
        }

        obj.put("queueSpot", spot);

        status.put(id, obj);
        return id;
    }

    public static JSONObject getStatus(String id){
        if(status.containsKey(id)) {
            JSONObject st = status.get(id);
            if(st.getString("status").equals("Done")) status.remove(id);
            return st;
        }
        JSONObject statusObj = new JSONObject();
        statusObj.put("id", id);
        statusObj.put("status", "Not Found.");
        return statusObj;
    }

    public static BufferedImage getResult(String id){
        BufferedImage resultImg = results.getOrDefault(id,null);
        if(resultImg != null) {
            results.remove(id);
            status.remove(id);
        }
        return resultImg;
    }

    public static void startScheduler(){
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    JSONObject wait = null;
                    Iterator<String> it = status.keySet().iterator();
                    String id;
                    while (it.hasNext()) {
                        id = it.next();
                        wait = status.get(id);
                        if(wait.getInt("queueSpot") == 0 && wait.getString("status").equals("In Queue")){
                            break;
                        } else {
                            wait = null;
                        }
                    }

                    if(wait == null) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        continue;
                    }

                    wait.put("status", "switching_model");
                    wait.put("progress", 0f);
                    wait.put("sampling_step",0);
                    wait.put("sampling_steps",1);
                    wait.put("eta_relative",0f);
                    status.put(wait.getString("id"), wait);

                    String positivePromptPreset = wait.optString("positivePromptPreset","");

                    String positivePrompt = presetNameDisplayMappings.getOrDefault(positivePromptPreset,new JSONObject()).optString("positivePrompt","");
                    String negativePrompt = presetNameDisplayMappings.getOrDefault(positivePromptPreset,new JSONObject()).optString("negativePrompt","");

                    try{
                        positivePrompt = positivePrompt + ", " + wait.getString("positivePrompt");
                        negativePrompt = negativePrompt + ", " + wait.getString("negativePrompt");

                        URL url = new URL("http://" + staticIP + ":7860/sdapi/v1/txt2img");
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("POST");
                        connection.setRequestProperty("Content-Type", "application/json");
                        connection.setDoOutput(true);

                        int width = wait.getInt("width");
                        int height = wait.getInt("height");
                        int steps = calculateSteps(width,height);
                        steps = 30;

                        JSONObject requestJson = new JSONObject();
                        requestJson.put("prompt", positivePrompt);
                        requestJson.put("negative_prompt", negativePrompt);
                        requestJson.put("steps", steps);
                        requestJson.put("sampler_name", "DPM++ 2M");
                        requestJson.put("sampler_name", "Euler a");
                        requestJson.put("cfg_scale", 7.5);
                        requestJson.put("width", width);
                        requestJson.put("height", height);
                        requestJson.put("sd_model_checkpoint", wait.getString("model"));
                        requestJson.put("batch_size", 1);
                        requestJson.put("seed", wait.getInt("seed"));

                        ensureModelLoaded(wait.getString("model"));

                        wait.put("status", "Processing");
                        status.put(wait.getString("id"), wait);

                        String jsonInputString = requestJson.toString();

                        try (OutputStream os = connection.getOutputStream()) {
                            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                            os.write(input, 0, input.length);
                        }

                        JSONObject finalWait = wait;
                        Thread progressThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                float progress = 0;
                                int errors = 0;
                                while (true) {
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    try{
                                        final JSONObject waitFinal = finalWait;
                                        URL progressUrl = new URL("http://" + staticIP + ":7860/sdapi/v1/progress");
                                        HttpURLConnection progressConnection = (HttpURLConnection) progressUrl.openConnection();
                                        progressConnection.setRequestMethod("GET");

                                        String progressBody;
                                        try (Scanner scanner = new Scanner(progressConnection.getInputStream(), StandardCharsets.UTF_8)) {
                                            progressBody = scanner.useDelimiter("\\A").next();
                                        }

                                        JSONObject pr = new JSONObject(progressBody);

                                        progress = (float) pr.getDouble("progress");
                                        waitFinal.put("sampling_step",pr.getJSONObject("state").getInt("sampling_step"));
                                        waitFinal.put("sampling_steps",pr.getJSONObject("state").getInt("sampling_steps"));
                                        waitFinal.put("eta_relative",(float) pr.getDouble("eta_relative"));

                                        waitFinal.put("progress", progress);
                                        status.put(waitFinal.getString("id"), waitFinal);

                                        if(waitFinal.getInt("sampling_step") == waitFinal.getInt("sampling_steps")){
                                            break;
                                        }
                                        errors = 0;
                                    }catch (Exception e){
                                        e.printStackTrace();
                                        errors++;
                                        if(errors >= 5) break;
                                    }
                                }
                            }
                        });
                        progressThread.start();

                        String responseBody;
                        try (Scanner scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8)) {
                            responseBody = scanner.useDelimiter("\\A").next();
                        }

                        String base64Image = responseBody.split("\"")[3];
                        byte[] imageBytes = Base64.getDecoder().decode(base64Image);

                        try (ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes)) {
                            results.put(wait.getString("id"),ImageIO.read(bis));
                        }

                        //ImageIO.write(results.get(wait.getString("id")),"PNG",new File("nudes/sd/" + UUID.randomUUID() + "_output.png"));

                        wait.put("status", "Done");
                        status.put(wait.getString("id"), wait);

                        for(String s : status.keySet()){
                            JSONObject temp = status.get(s);
                            temp.put("queueSpot", temp.getInt("queueSpot") - 1);
                            status.put(temp.getString("id"), temp);
                        }
                    } catch (Exception e){
                        wait.put("status", "Error");
                        wait.put("error", ChatEvent.exceptionToString(e));
                        status.put(wait.getString("id"), wait);

                        e.printStackTrace();

                        for(String s : status.keySet()){
                            wait = status.get(s);
                            wait.put("queueSpot", wait.getInt("queueSpot") - 1);
                            status.put(wait.getString("id"), wait);
                        }
                    }
                }
            }
        });
        t.start();
    }

    public static void reload(){
        try {
            String json = new String(StableDiffusion.class.getClassLoader().getResourceAsStream("presets.json").readAllBytes());
            JSONArray jsonArray = new JSONArray(json);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject preset = jsonArray.getJSONObject(i);
                presetNameMappings.put(preset.getString("internalName"), preset);
                presetNameDisplayMappings.put(preset.getString("displayName"), preset);
            }

            json = new String(StableDiffusion.class.getClassLoader().getResourceAsStream("models.json").readAllBytes());
            jsonArray = new JSONArray(json);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject model = jsonArray.getJSONObject(i);
                modelMappings.put(model.getString("display"),model.getString("file"));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static int calculateSteps(int width, int height) {
        int size = Math.max(width, height);
        double base = 30.0;
        double penalty = Math.pow((size / 512.0), 2) * 1.5; // quadratic penalty

        int steps = (int) (base - penalty);

        if (steps < 16) {
            steps = 16; // minimum safe steps
        }

        return (int) (steps * 1.0);
    }

    public static void ensureModelLoaded(String desiredModel) throws IOException, InterruptedException {
        String currentModel = getCurrentModel();
        System.out.println("Current model: " + currentModel);

        if (!currentModel.equals(desiredModel)) {
            System.out.println("Switching model to: " + desiredModel);
            switchModel(desiredModel);

            // Wait a little bit for the model to fully load (VERY IMPORTANT)
            Thread.sleep(5000); // You can increase if needed (e.g., 10000ms = 10s for large models)

            System.out.println("Model switched successfully!");
        } else {
            System.out.println("Model is already correct, no switch needed!");
        }
    }

    private static String getCurrentModel() throws IOException {
        URL url = new URL("http://" + staticIP + ":7860/sdapi/v1/options");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            content.append(line);
        }
        in.close();

        JSONObject json = new JSONObject(content.toString());
        return json.getString("sd_model_checkpoint");
    }

    private static void switchModel(String modelName) throws IOException {
        URL url = new URL("http://" + staticIP + ":7860/sdapi/v1/options");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);

        JSONObject json = new JSONObject();
        json.put("sd_model_checkpoint", modelName);

        try (OutputStream os = con.getOutputStream()) {
            byte[] input = json.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = con.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Failed to switch model! HTTP " + responseCode);
        }
    }
}
