package me.redstoner2019.upscale;

import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import static me.redstoner2019.chatgpt.StableDiffusion.staticIP;

public class Upscale {
    public static BufferedImage upscaleImage(BufferedImage inputImage, double scaleFactor, String upscalerName) throws IOException {
        // Convert BufferedImage to Base64 PNG
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(inputImage, "png", baos);
        baos.flush();
        String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
        baos.close();

        // Create JSON payload
        JSONObject requestJson = new JSONObject();
        requestJson.put("image", "data:image/png;base64," + base64Image);
        requestJson.put("upscaling_resize", scaleFactor);
        requestJson.put("upscaler_1", upscalerName); // e.g. "R-ESRGAN 4x+"

        // Send request to /sdapi/v1/extra-single-image
        URL url = new URL("http://" + staticIP + ":7860/sdapi/v1/extra-single-image");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);

        try (OutputStream os = con.getOutputStream()) {
            byte[] input = requestJson.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        // Read response
        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
        }

        // Parse base64 result
        JSONObject resultJson = new JSONObject(response.toString());
        String base64Result = resultJson.getString("image");

        // Convert base64 back to BufferedImage
        byte[] imageBytes = Base64.getDecoder().decode(base64Result);
        InputStream is = new ByteArrayInputStream(imageBytes);
        return ImageIO.read(is);
    }

    public static void main(String[] args) throws IOException {
        BufferedImage upscaled = upscaleImage(ImageIO.read(new File("C:\\Users\\Redstoner_2019\\Downloads\\image - 2025-04-29T110106.868.png")),2.0, "R-ESRGAN 4x+");
        ImageIO.write(upscaled,"png",new File("upscaled.png"));
    }
}
