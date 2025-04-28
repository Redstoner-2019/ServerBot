package me.redstoner2019.chatgpt;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Scanner;
import java.util.UUID;

public class SDUITest {

    private static JTextField positivePromptField;
    private static JTextField negativePromptField;
    private static JProgressBar progressBar;
    private static JButton startButton;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SDUITest::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Stable Diffusion Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 250);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(5, 1, 5, 5));

        positivePromptField = new JTextField("1girl, masterpiece, beautiful");
        negativePromptField = new JTextField("bad anatomy, lowres");

        startButton = new JButton("Start Generation");
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        panel.add(new JLabel("Positive Prompt:"));
        panel.add(positivePromptField);
        panel.add(new JLabel("Negative Prompt:"));
        panel.add(negativePromptField);
        panel.add(startButton);
        panel.add(progressBar);

        frame.getContentPane().add(panel);
        frame.setVisible(true);

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(SDUITest::startGeneration).start();
            }
        });
    }

    private static void startGeneration() {
        try {
            String prompt = positivePromptField.getText();
            String negativePrompt = negativePromptField.getText();

            // Start txt2img request
            URL url = new URL("http://127.0.0.1:7860/sdapi/v1/txt2img");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            String jsonInputString = String.format(
                    "{" +
                            "\"prompt\": \"%s\"," +
                            "\"negative_prompt\": \"%s\"," +
                            "\"steps\": 30," +
                            "\"sampler_name\": \"DPM++ 2M\"," +
                            "\"cfg_scale\": 7.5," +
                            "\"width\": 512," +
                            "\"height\": 768," +
                            "\"batch_size\": 1" +
                            "}",
                    prompt, negativePrompt
            );

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        // Poll the progress endpoint
                        boolean generating = true;
                        System.out.println("Starting generation...");
                        while (generating) {
                            Thread.sleep(500);

                            URL progressUrl = new URL("http://127.0.0.1:7860/sdapi/v1/progress");
                            HttpURLConnection progressConnection = (HttpURLConnection) progressUrl.openConnection();
                            progressConnection.setRequestMethod("GET");

                            String progressBody;
                            try (Scanner scanner = new Scanner(progressConnection.getInputStream(), StandardCharsets.UTF_8)) {
                                progressBody = scanner.useDelimiter("\\A").next();
                            }

                            double progress = extractValue(progressBody, "progress");
                            progressBar.setValue((int) (progress * 100));

                            if (progress >= 1.0) {
                                generating = false;
                            }
                        }

                        System.out.println("Done generation!");
                    }catch (Exception e){

                    }
                }
            });
            t.start();


            // Get final image response
            String responseBody;
            try (Scanner scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8)) {
                responseBody = scanner.useDelimiter("\\A").next();
            }

            String base64Image = responseBody.split("\"")[3];
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);

            try (FileOutputStream fos = new FileOutputStream("nudes/sd/" + UUID.randomUUID() + "_output.png")) {
                fos.write(imageBytes);
            }

            JOptionPane.showMessageDialog(null, "Image saved as output.png!");
            progressBar.setValue(100);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + ex.getMessage());
        }
    }

    private static double extractValue(String json, String key) {
        int index = json.indexOf(key);
        if (index == -1) return 0.0;
        int start = json.indexOf(':', index) + 1;
        int end = json.indexOf(',', start);
        if (end == -1) end = json.indexOf('}', start);
        return Double.parseDouble(json.substring(start, end).trim());
    }
}
