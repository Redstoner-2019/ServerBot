package me.redstoner2019.events;

import me.redstoner2019.Main;
import me.redstoner2019.chatgpt.StableDiffusion;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static me.redstoner2019.chatgpt.Ollama.askModel;

public class SDSlashCommand extends ListenerAdapter {

    public static HashMap<String,String> settingsMap = new HashMap<>();

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        if(event.getName().equals("generateimage")){
            String model = event.getOption("model").getAsString();
            String positivePrompt = event.getOption("positiveprompt").getAsString();
            String negativePrompt = event.getOption("negativeprompt") != null ? event.getOption("negativeprompt").getAsString() : "";
            String basePromptPreset = event.getOption("basepromtpreset") != null ? event.getOption("basepromtpreset").getAsString() : "";
            String sizeString = event.getOption("size") != null ? event.getOption("size").getAsString() : "MEDIUM";

            startGeneration(event.getHook(), model,positivePrompt,negativePrompt,basePromptPreset,sizeString);
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String[] parts = event.getComponentId().split(":");
        String action = parts[0];
        String id = parts[1];
        if (action.equals("regenerate")) {
            parts = settingsMap.get(id).split(":");
            String model = new String(Base64.decodeBase64(parts[0]));
            String positivePrompt = new String(Base64.decodeBase64(parts[1]));
            String negativePrompt = new String(Base64.decodeBase64(parts[2]));
            String basePrompt = new String(Base64.decodeBase64(parts[3]));
            String sizeString = new String(Base64.decodeBase64(parts[4]));
            event.deferReply().queue();
            startGeneration(event.getHook(), model,positivePrompt,negativePrompt,basePrompt,sizeString);
        }
    }


    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (event.getName().equals("generateimage") && event.getFocusedOption().getName().equals("model")) {
            List<String> suggestions = new ArrayList<>(List.of("Stable Diffusion","Stable Diffusion XL", "AnythingXL"));

            event.replyChoices(suggestions.stream()
                    .map(choice -> new net.dv8tion.jda.api.interactions.commands.Command.Choice(choice, choice))
                    .toList()
            ).queue();
        }
        if (event.getName().equals("generateimage") && event.getFocusedOption().getName().equals("basepromtpreset")) {
            List<String> suggestions = new ArrayList<>(StableDiffusion.presetNameDisplayMappings.keySet());

            List<String> filteredSuggestions = suggestions.stream()
                    .filter(s -> s.toLowerCase().startsWith(event.getFocusedOption().getValue().toLowerCase()))
                    .limit(25)
                    .toList();

            event.replyChoices(filteredSuggestions.stream()
                    .map(choice -> new net.dv8tion.jda.api.interactions.commands.Command.Choice(choice, choice))
                    .toList()
            ).queue();
        }
        if (event.getName().equals("generateimage") && event.getFocusedOption().getName().equals("size")) {
            List<String> suggestions = new ArrayList<>(List.of("LARGE","MEDIUM","SMALL","MASSIVE"));

            List<String> filteredSuggestions = suggestions.stream()
                    .filter(s -> s.toLowerCase().startsWith(event.getFocusedOption().getValue().toLowerCase()))
                    .limit(25)
                    .toList();

            event.replyChoices(filteredSuggestions.stream()
                    .map(choice -> new net.dv8tion.jda.api.interactions.commands.Command.Choice(choice, choice))
                    .toList()
            ).queue();
        }
    }

    public static void startGeneration(InteractionHook hook, String model, String positivePrompt, String negativePrompt, String basePromptPreset, String sizeString){
        String modelName;

        switch (model) {
            case "Stable Diffusion" -> {
                modelName = "v1-5-pruned-emaonly.safetensors";
            }
            case "Stable Diffusion XL" -> {
                modelName = "sd_xl_base_1.0.safetensors";
            }
            case "AnythingXL" -> {
                modelName = "AnythingXL_xl.safetensors";
            }
            default -> {
                model = "Stable Diffusion";
                modelName = "v1-5-pruned-emaonly.safetensors";
                return;
            }
        }

        int size = 512;
        switch (sizeString) {
            case "SMALL" -> {
                size = 384;
            }
            case "LARGE" -> {
                size = 1024;
            }
            case "MASSIVE" -> {
                size = 1536;
            }
        }

        String id = StableDiffusion.schedule(modelName,positivePrompt, negativePrompt,basePromptPreset,size);

        int finalSize = size;
        boolean running = true;
        while (running) {
            JSONObject status = StableDiffusion.getStatus(id);
            switch (status.getString("status")) {
                case "In Queue" -> {
                    EmbedBuilder embedBuilder = new EmbedBuilder()
                            .setTitle("Waiting in Queue...")
                            .addField("Your Spot:",status.getInt("queueSpot") + "",false)
                            .addField("","",false)
                            .addField("Model:", "`" + model + "`", false)
                            .addField("Preset:", "`" + (basePromptPreset.isEmpty() ? "None" : basePromptPreset) + "`", false)
                            .addField("Positive Prompt:", "`" + positivePrompt + "`", false)
                            .addField("Negative Prompt:", "`" + (negativePrompt.isEmpty() ? " " : negativePrompt) + "`", false)
                            .addField("Image Size:", "`" + finalSize + " x " + finalSize + "`", false)
                            .setColor(Color.ORANGE);

                    hook.editOriginalEmbeds(embedBuilder.build()).queue();
                }
                case "Processing" -> {
                    EmbedBuilder embedBuilder = new EmbedBuilder()
                            .setTitle("Processing...")
                            .addField("Progress:",getProgressBar(status.getFloat("progress")),false)
                            .addField("Sampling:","[ " + status.getInt("sampling_step") + " / " + status.getInt("sampling_steps") + " ]",false)
                            .addField("Estimated time left:",formatEta(status.getFloat("eta_relative")),false)
                            .addField("","",false)
                            .addField("Model:", "`" + model + "`", false)
                            .addField("Preset:", "`" + (basePromptPreset.isEmpty() ? "None" : basePromptPreset) + "`", false)
                            .addField("Positive Prompt:", "`" + positivePrompt + "`", false)
                            .addField("Negative Prompt:", "`" + (negativePrompt.isEmpty() ? " " : negativePrompt) + "`", false)
                            .addField("Image Size:", "`" + finalSize + " x " + finalSize + "`", false)
                            .setColor(Color.YELLOW);

                    hook.editOriginalEmbeds(embedBuilder.build()).queue();
                }
                case "Done" -> {
                    running = false;

                    String settingsString = Base64.encodeBase64String(model.getBytes()) + ":"
                            + Base64.encodeBase64String(positivePrompt.getBytes()) + ":"
                            + Base64.encodeBase64String(negativePrompt.getBytes()) + ":"
                            + Base64.encodeBase64String(basePromptPreset.getBytes()) + ":"
                            + Base64.encodeBase64String(sizeString.getBytes());
                    String newUUID = UUID.randomUUID().toString();

                    settingsMap.put(newUUID,settingsString);

                    Button regenerate = Button.primary("regenerate:" + newUUID, "Generate again");

                    EmbedBuilder embedBuilder = new EmbedBuilder()
                            .setTitle("Uploading...")
                            .addField("Model:           ", "`" + model + "`", false)
                            .addField("Preset:          ", "`" + (basePromptPreset.isEmpty() ? "None" : basePromptPreset) + "`", false)
                            .addField("Positive Prompt: ", "`" + positivePrompt + "`", false)
                            .addField("Negative Prompt: ", "`" + (negativePrompt.isEmpty() ? " " : negativePrompt) + "`", false)
                            .addField("Image Size:      ", "`" + finalSize + " x " + finalSize + "`", false)
                            .addField("Uploading Image...","",false)
                            .setColor(Color.GREEN);

                    hook.editOriginalEmbeds(embedBuilder.build())
                            .setActionRow(regenerate)
                            .queue();

                    BufferedImage image = StableDiffusion.getResult(id);
                    BufferedImage thumbnail = createThumbnail(image,192,192);

                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    try {
                        ImageIO.write(image, "png", os);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    byte[] imageData = os.toByteArray();

                    os = new ByteArrayOutputStream();
                    try {
                        ImageIO.write(thumbnail, "png", os);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    byte[] thumbnailData = os.toByteArray();

                    String userPrompt = "You are an AI moderation assistant.\n" +
                            "\n" +
                            "You MUST answer with ONLY \"YES\" or \"NO\" and NOTHING ELSE.\n" +
                            "\n" +
                            "If the following text contains nudity, sexual themes, erotic descriptions, or explicit adult content, answer \"YES\".\n" +
                            "\n" +
                            "If the text does not, answer \"NO\".\n" +
                            "\n" +
                            "Here is the text:\n" + positivePrompt;

                    /*String result = "Error";
                    try {
                        result = askModel("tinyllama", userPrompt);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }*/

                    embedBuilder = new EmbedBuilder()
                            .setTitle("Done!")
                            .addField("Model:           ", "`" + model + "`", false)
                            .addField("Preset:          ", "`" + (basePromptPreset.isEmpty() ? "None" : basePromptPreset) + "`", false)
                            .addField("Positive Prompt: ", "`" + positivePrompt + "`", false)
                            .addField("Negative Prompt: ", "`" + (negativePrompt.isEmpty() ? " " : negativePrompt) + "`", false)
                            .addField("Image Size:      ", "`" + image.getWidth() + " x " + image.getHeight() + "`", false)
                            //.addField("NSFW",result,false)
                            .setColor(Color.GREEN)
                            .setThumbnail("attachment://thumbnail.png")
                            .setImage("attachment://image.png");

                    hook.editOriginalEmbeds(embedBuilder.build())
                            .setFiles(FileUpload.fromData(thumbnailData,"thumbnail.png"))
                            .setFiles(FileUpload.fromData(imageData,"image.png"))
                            .setActionRow(regenerate)
                            .queue();

                    hook.editOriginal("").queue();
                }
                case "Error" -> {
                    EmbedBuilder embedBuilder = new EmbedBuilder()
                            .setTitle("An Error occured.")
                            .addField("Model:", "`" + model + "`", false)
                            .addField("Preset:", "`" + (basePromptPreset.isEmpty() ? "None" : basePromptPreset) + "`", false)
                            .addField("Positive Prompt:", "`" + positivePrompt + "`", false)
                            .addField("Negative Prompt:", "`" + (negativePrompt.isEmpty() ? " " : negativePrompt) + "`", false)
                            .addField("Image Size:", "`" + finalSize + " x " + finalSize + "`", false)
                            .addField("","",false)
                            .addField("Error:", status.toString(3), false)
                            .setColor(Color.RED);

                    hook.editOriginalEmbeds(embedBuilder.build()).queue();
                    hook.editOriginal("An error occured. \n" + status.toString(3)).queue();
                    running = false;
                }
            }

            try {
                Thread.sleep(750);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static BufferedImage createThumbnail(BufferedImage originalImage, int width, int height) {
        Image scaledImage = originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage thumbnail = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = thumbnail.createGraphics();
        g2d.drawImage(scaledImage, 0, 0, null);
        g2d.dispose();

        return thumbnail;
    }

    public static String getProgressBar(float progress) {
        int barLength = 20;
        int totalBlocks = barLength * 4;
        int filledBlocks = Math.round(progress * totalBlocks);

        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < barLength; i++) {
            int blockValue = Math.min(filledBlocks - (i * 4), 4);
            if (blockValue >= 4) {
                bar.append("█");
            } else if (blockValue == 3) {
                bar.append("▓");
            } else if (blockValue == 2) {
                bar.append("▒");
            } else if (blockValue == 1) {
                bar.append("░");
            } else {
                bar.append(" ");
            }
        }
        bar.append("] ");
        bar.append(String.format(" %.2f%%", progress * 100));

        return bar.toString();
    }

    public static String formatEta(float seconds) {
        int totalSeconds = Math.round(seconds);

        if (totalSeconds < 60) {
            return totalSeconds + "s";
        } else {
            int minutes = totalSeconds / 60;
            int remainingSeconds = totalSeconds % 60;
            return String.format("%dm:%02ds", minutes, remainingSeconds);
        }
    }
}
