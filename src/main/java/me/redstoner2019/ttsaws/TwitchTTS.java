package me.redstoner2019.ttsaws;

import me.redstoner2019.Main;
import org.apache.commons.codec.binary.Base64;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.polly.model.*;

import software.amazon.awssdk.core.ResponseBytes;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.List;

public class TwitchTTS {
    private static final String ACCESS_KEY = new String(Base64.decodeBase64("QUtJQVQ0R1ZSSjdIUTdEQks2MjY="));
    private static final String SECRET_KEY = new String(Base64.decodeBase64("bmpDR1I3SXg5R3JRVklMWW53Qk5YcUVSU2lpNVVObHlYMFhqbzVGcA=="));

    public static byte[] generateTTSStream(String text, VoiceId voice, float speed, float pitch, boolean ssml) {
        PollyClient polly = createPollyClient();

        if(text.contains("<") || text.contains(">")){
            ssml = false;
        }

        String ssmlText = "<speak><prosody rate=\"" + (int) speed + "%\" pitch=\"" + (int) pitch + "%\">" + text + "</prosody></speak>";

        SynthesizeSpeechRequest request;

        if(getRequiredEngine(voice.toString()).equals("neural")) ssml = false;

        if(ssml){
            request = SynthesizeSpeechRequest.builder()
                    .text(ssmlText)
                    .textType(TextType.SSML)
                    .voiceId(voice)
                    .engine(getRequiredEngine(voice.toString()))
                    .outputFormat(OutputFormat.MP3)
                    .build();
        } else {
            request = SynthesizeSpeechRequest.builder()
                    .text(text)
                    .voiceId(voice)
                    .engine(getRequiredEngine(voice.toString()))
                    .outputFormat(OutputFormat.MP3)
                    .build();
        }

        try {
            ResponseInputStream<SynthesizeSpeechResponse> response = polly.synthesizeSpeech(request);
            InputStream audioStream = response;

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[4096];
            int bytesRead;
            while ((bytesRead = audioStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, bytesRead);
            }

            buffer.flush();
            polly.close();
            return buffer.toByteArray(); // Return byte array instead of a file

        } catch (Exception e) {
            System.err.println(ssmlText);
            System.err.println(getRequiredEngine(voice.toString()));
            System.err.println(voice);
            System.err.println(speed);
            System.err.println(pitch);
            e.printStackTrace();
            if(ssml) return generateTTSStream(text, voice,speed,pitch, false);
            return new byte[0];
        }
    }

    public static String generateTTS(String text, String filename, VoiceId voice, float speed, float pitch) {
        try {
            byte[] audioStream = generateTTSStream(text, voice,speed,pitch, true);

            File file = new File(filename);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(audioStream);
            fos.close();

            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getRequiredEngine(String voiceId) {
        PollyClient polly = createPollyClient();

        try {
            DescribeVoicesRequest request = DescribeVoicesRequest.builder().build();
            DescribeVoicesResponse response = polly.describeVoices(request);

            List<Voice> voices = response.voices();
            for (Voice voice : voices) {
                //System.out.println(voice.id().name() + " Engines: " + voice.supportedEngines());
                if (voice.id().toString().equalsIgnoreCase(voiceId)) {
                    if (voice.supportedEngines().contains(Engine.STANDARD)) {
                        return "standard";
                    } else if(voice.supportedEngines().contains(Engine.NEURAL)){
                        return "neural";
                    } else {
                        return "standard";
                    }
                }
            }
        } catch (PollyException e) {
            e.printStackTrace();
        }

        return "standard"; // Default to standard if not found
    }

    public static PollyClient createPollyClient() {
        return PollyClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)))
                .build();
    }

    public static void main(String[] args) {
        //String mp3Path = generateTTS("Hewwo! UwU, I'm a wittwe kitten nya~!","tts.mp3");
        //System.out.println("Generated TTS file: " + mp3Path);
    }
}
