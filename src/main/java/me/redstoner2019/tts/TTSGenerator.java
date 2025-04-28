package me.redstoner2019.tts;

import java.io.*;

public class TTSGenerator {
    public static String generateTTS(String text) {
        try {
            // Run the Python script with the text as an argument
            ProcessBuilder processBuilder = new ProcessBuilder("python", "tts.py", text);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Wait for the process to finish
            process.waitFor();

            System.out.println(new String(process.getInputStream().readAllBytes()));

            // Return the MP3 file path
            return "tts.mp3";
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
}

