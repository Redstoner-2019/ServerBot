package me.redstoner2019.chatgpt;

import me.redstoner2019.Main;
import me.redstoner2019.events.ChatEvent;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.swing.*;
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

    static {
        try {
            String json = new String(StableDiffusion.class.getClassLoader().getResourceAsStream("presets.json").readAllBytes());
            JSONArray jsonArray = new JSONArray(json);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject preset = jsonArray.getJSONObject(i);
                presetNameMappings.put(preset.getString("internalName"), preset);
                presetNameDisplayMappings.put(preset.getString("displayName"), preset);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * C:\Users\Redstoner_2019\Projects\StableDiffusion
     * cd Projects/StableDiffusion/stable-diffusion-webui
     * python -m venv venv
     * venv\Scripts\activate
     * python launch.py --api
     */

    public static String schedule(String model, String positivePrompt, String negativePrompt, String preset, int size){
        String id = UUID.randomUUID().toString();
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("model", model);
        obj.put("positivePrompt", positivePrompt);
        obj.put("negativePrompt", negativePrompt);
        obj.put("positivePromptPreset", preset);
        obj.put("status", "In Queue");
        obj.put("size", size);

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

                    wait.put("status", "Processing");
                    wait.put("progress", 0f);
                    wait.put("sampling_step",0);
                    wait.put("sampling_steps",1);
                    wait.put("eta_relative",-1f);
                    status.put(wait.getString("id"), wait);

                    String positivePromptPreset = wait.optString("positivePromptPreset","");

                    String positivePrompt = presetNameDisplayMappings.getOrDefault(positivePromptPreset,new JSONObject()).optString("positivePrompt","");
                    String negativePrompt = presetNameDisplayMappings.getOrDefault(positivePromptPreset,new JSONObject()).optString("negativePrompt","");

                    /*switch (positivePromptPreset) {
                        case "default" -> {
                            negativePrompt = "lowres, bad anatomy, bad hands, bad fingers, blurry, bad face, deformed, mutated, extra limbs, missing fingers, cloned face, poorly drawn hands, watermark, signature, text, worst quality, jpeg artifacts, ";
                        }
                        case "Anime Girl Preset" -> {
                            positivePrompt = "1girl, solo, masterpiece, best quality, ultra-detailed, highly detailed eyes, beautiful detailed face, perfect anatomy, detailed hair, looking at viewer, dynamic pose, (soft lighting), intricate clothes, fantasy background, vibrant colors, cinematic lighting, ";
                            negativePrompt = "lowres, bad anatomy, bad hands, bad fingers, blurry, bad face, deformed, mutated, extra limbs, missing fingers, cloned face, poorly drawn hands, watermark, signature, text, worst quality, jpeg artifacts, ";
                        }
                    }*/

                    try{
                        positivePrompt = positivePrompt + wait.getString("positivePrompt");
                        negativePrompt = negativePrompt + wait.getString("negativePrompt");

                        URL url = new URL("http://127.0.0.1:7860/sdapi/v1/txt2img");
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("POST");
                        connection.setRequestProperty("Content-Type", "application/json");
                        connection.setDoOutput(true);

                        int size = wait.getInt("size");
                        int steps = calculateSteps(size,size);

                        JSONObject requestJson = new JSONObject();
                        requestJson.put("prompt", positivePrompt);
                        requestJson.put("negative_prompt", negativePrompt);
                        requestJson.put("steps", steps);
                        requestJson.put("sampler_name", "DPM++ 2M");
                        requestJson.put("cfg_scale", 7.5);
                        requestJson.put("width", size);
                        requestJson.put("height", size);
                        requestJson.put("sd_model_checkpoint", wait.getString("model"));
                        requestJson.put("batch_size", 1);

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
                                while (true) {
                                    final JSONObject waitFinal = finalWait;
                                    progress+= (float) Math.random() * 5;
                                    progress = Math.min(progress,100);

                                    try{
                                        URL progressUrl = new URL("http://127.0.0.1:7860/sdapi/v1/progress");
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
                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }

                                    waitFinal.put("progress", progress);
                                    status.put(waitFinal.getString("id"), waitFinal);

                                    if(waitFinal.getInt("sampling_step") == waitFinal.getInt("sampling_steps")){
                                        break;
                                    }
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
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
                        wait.put("message", ChatEvent.exceptionToString(e));
                        e.printStackTrace();
                        status.put(wait.getString("id"), wait);

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

    public static int calculateSteps(int width, int height) {
        int size = Math.max(width, height);
        double base = 30.0;
        double penalty = Math.pow((size / 512.0), 2) * 1.5; // quadratic penalty

        int steps = (int) (base - penalty);

        if (steps < 16) {
            steps = 16; // minimum safe steps
        }

        return (int) (steps * 1.2);
    }

}
