package me.redstoner2019.chatgpt;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Scanner;

public class StableDiffusionTest {
    public static void main(String[] args) throws Exception {
        URL url = new URL("http://127.0.0.1:7860/sdapi/v1/txt2img");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        String prompt = "1girl, solo, masterpiece, best quality, ultra-detailed, highly detailed eyes, beautiful detailed face, perfect anatomy, detailed hair, looking at viewer, dynamic pose, (soft lighting), intricate clothes, fantasy background";
        String negativePrompt = "lowres, bad anatomy, bad hands, bad fingers, blurry, bad face, deformed, mutated, extra limbs, missing fingers, cloned face, poorly drawn hands, watermark, signature, text, worst quality, jpeg artifacts";

        String jsonInputString = String.format(
                "{"
                        + "\"prompt\": \"%s\","
                        + "\"negative_prompt\": \"%s\","
                        + "\"steps\": 30,"
                        + "\"sampler_name\": \"DPM++ 2M\","
                        + "\"cfg_scale\": 7.5,"
                        + "\"width\": 1024,"
                        + "\"height\": 1024,"
                        + "\"batch_size\": 1"
                        + "}",
                prompt, negativePrompt
        );

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        String responseBody;
        try (Scanner scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8)) {
            responseBody = scanner.useDelimiter("\\A").next();
        }

        // Extract base64 image
        String base64Image = responseBody.split("\"")[3]; // crude but simple extraction
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);

        // Save image
        try (FileOutputStream fos = new FileOutputStream("output.png")) {
            fos.write(imageBytes);
        }

        System.out.println("Image saved as output.png!");
    }
}
