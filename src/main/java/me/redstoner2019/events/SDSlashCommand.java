package me.redstoner2019.events;

import me.redstoner2019.Main;
import me.redstoner2019.chatgpt.StableDiffusion;
import me.redstoner2019.upscale.Upscale;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

import static me.redstoner2019.chatgpt.Ollama.askModel;

public class SDSlashCommand extends ListenerAdapter {

    public static HashMap<String,String> settingsMap = new HashMap<>();

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if(event.getName().equals("generateimage")){
            event.deferReply().queue();
            String model = event.getOption("model").getAsString();
            String positivePrompt = event.getOption("positiveprompt").getAsString();
            String negativePrompt = event.getOption("negativeprompt") != null ? event.getOption("negativeprompt").getAsString() : "";
            String basePromptPreset = event.getOption("basepromtpreset") != null ? event.getOption("basepromtpreset").getAsString() : "";
            String sizeString = event.getOption("size") != null ? event.getOption("size").getAsString() : "MEDIUM";
            String seedString = event.getOption("seed") != null ? event.getOption("seed").getAsString() : "-1";
            int width = event.getOption("width") != null ? event.getOption("width").getAsInt() : 128;
            int height = event.getOption("height") != null ? event.getOption("height").getAsInt() : 128;

            event.getHook().sendMessage("Waiting...").queue(message -> {
                startGeneration(message, model,positivePrompt,negativePrompt,basePromptPreset,sizeString,width,height,seedString);
            });
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String[] parts = event.getComponentId().split(":");
        String action = parts[0];

        if(action.equals("upscale")){
            event.deferReply().queue(message -> {
                try{
                    URL url = new URL(event.getMessage().getEmbeds().get(0).getImage().getUrl());

                    BufferedImage image = ImageIO.read(url);
                    BufferedImage upscaled = Upscale.upscaleImage(image,2.0, "R-ESRGAN 4x+");

                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    try {
                        ImageIO.write(upscaled, "png", os);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    byte[] imageData = os.toByteArray();

                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setTitle("Upscaled: ");
                    eb.addField("New Size: ","`" + upscaled.getWidth() + " x " + upscaled.getHeight() + "`", false);
                    eb.setColor(Color.GREEN);
                    eb.setImage("attachment://upscaled_" + event.getMessage().getId() + ".png");
                    message.editOriginalEmbeds(eb.build())
                            .setFiles(FileUpload.fromData(imageData,"upscaled_" + event.getMessage().getId() + ".png"))
                            .queue();
                }catch (Exception e) {

                }
            });

        }

        if(action.equals("delete")){
            if(parts.length == 1){
                Button deleteConfirm = Button.danger("delete:" + event.getMessageId(),"Confirm Delete");

                event.getInteraction()
                        .reply("Are you sure you want to delete?")
                        .setActionRow(deleteConfirm)
                        .setEphemeral(true)
                        .queue();
            } else {
                String id = parts[1];
                event.getMessage().delete().queue();
                event.getChannel().retrieveMessageById(id).queue(message -> {
                    message.delete().queue();
                });
            }
        }

        if (action.equals("regenerate")) {
            String id = parts[1];
            if(!settingsMap.containsKey(id)) {
                event.reply("This is not valid anymore.").queue();
                return;
            }
            parts = settingsMap.get(id).split(":");
            String model = new String(Base64.decodeBase64(parts[0]));
            String positivePrompt = new String(Base64.decodeBase64(parts[1]));
            String negativePrompt = new String(Base64.decodeBase64(parts[2]));
            String basePrompt = new String(Base64.decodeBase64(parts[3]));
            String sizeString = new String(Base64.decodeBase64(parts[4]));
            String seedString = new String(Base64.decodeBase64(parts[5]));
            int width = byteArrayToInt(new String(Base64.decodeBase64(parts[6])).getBytes());
            int height = byteArrayToInt(new String(Base64.decodeBase64(parts[7])).getBytes());

            event.deferReply().queue();

            event.getHook().sendMessage("Waiting...").queue(message -> {
                startGeneration(message, model,positivePrompt,negativePrompt,basePrompt,sizeString,width,height,seedString);
            });
        }
    }


    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (event.getName().equals("generateimage") && event.getFocusedOption().getName().equals("model")) {
            List<String> suggestions = new ArrayList<>(StableDiffusion.modelMappings.keySet());

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
            List<String> suggestions = new ArrayList<>(List.of("MASSIVE","LARGE","HIGH","MEDIUM","SMALL"));
            if(!Main.GPU_DETECTED){
                suggestions = new ArrayList<>(List.of("HIGH","MEDIUM","SMALL"));
            }

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

    public static void startGeneration(Message hook, String model, String positivePrompt, String negativePrompt, String basePromptPreset, String sizeString, int width, int height, String seedString){
        String modelName = StableDiffusion.modelMappings.getOrDefault(model,"v1-5-pruned-emaonly.safetensors");
        switch (sizeString) {
            case "SMALL" -> {
                width = 384;
                height = 384;
            }
            case "HIGH" -> {
                width = 784;
                height = 784;
            }
            case "LARGE" -> {
                width = 1024;
                height = 1024;
            }
            case "MASSIVE" -> {
                width = 1536;
                height = 1536;
            }
            case "CUSTOM" -> {
            }
            default -> {
                width = 512;
                height = 512;
            }
        }

        int seed;

        try{
            seed = Integer.parseInt(seedString);
        } catch (Exception e){
            seed = -1;
        }

        if(seed < 0) {
            seed = new Random().nextInt(Integer.MAX_VALUE);
        }

        String id = StableDiffusion.schedule(modelName,positivePrompt, negativePrompt,basePromptPreset,width, height, seed);

        boolean running = true;

        int lastSpot = -1;
        final int widthf = width;
        final int heightf = height;
        final int seedf = seed;
        MessageEmbed oldEmbed = null;
        while (running) {
            try{
                JSONObject status = StableDiffusion.getStatus(id);
                switch (status.getString("status")) {
                    case "In Queue" -> {
                        EmbedBuilder embedBuilder = new EmbedBuilder()
                                .setTitle("Waiting in Queue...")
                                .addField("Your Spot:",lastSpot + "",false)
                                .addField("","",false)
                                .addField("Model:", "`" + model + "`", false)
                                .addField("Preset:", "`" + (basePromptPreset.isEmpty() ? "None" : basePromptPreset) + "`", false)
                                .addField("Positive Prompt:", "`" + positivePrompt + "`", false)
                                .addField("Negative Prompt:", "`" + (negativePrompt.isEmpty() ? " " : negativePrompt) + "`", false)
                                .addField("Image Size:", "`" + widthf + " x " + heightf + "`", false)
                                .addField("Seed:", "`" + seedf + "`", false)
                                .setColor(Color.ORANGE);

                        MessageEmbed embed = embedBuilder.build();

                        if(!embedsAreEqual(embed,oldEmbed)) {
                            hook.editMessageEmbeds(embedBuilder.build()).queue();
                            hook.editMessage("Updated "+ "<t:" + (System.currentTimeMillis() / 1000) + ":R>").queue();
                            oldEmbed = embed;
                        }
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
                                .addField("Image Size:", "`" + widthf + " x " + heightf + "`", false)
                                .addField("Seed:", "`" + seedf + "`", false)
                                //.addField("Last Update:","<t:" + (System.currentTimeMillis() / 1000) + ":R>",false)
                                .setColor(Color.YELLOW);

                        MessageEmbed embed = embedBuilder.build();

                        if(!embedsAreEqual(embed,oldEmbed)) {
                            hook.editMessageEmbeds(embedBuilder.build()).queue();
                            hook.editMessage("Updated "+ "<t:" + (System.currentTimeMillis() / 1000) + ":R>").queue();
                            oldEmbed = embed;
                        }
                    }
                    case "switching_model" -> {
                       EmbedBuilder embedBuilder = new EmbedBuilder()
                                .setTitle("Switching Model... This might take a moment.")
                                .addField("Model:", "`" + model + "`", false)
                                .addField("Preset:", "`" + (basePromptPreset.isEmpty() ? "None" : basePromptPreset) + "`", false)
                                .addField("Positive Prompt:", "`" + positivePrompt + "`", false)
                                .addField("Negative Prompt:", "`" + (negativePrompt.isEmpty() ? " " : negativePrompt) + "`", false)
                                .addField("Image Size:", "`" + widthf + " x " + heightf + "`", false)
                               .addField("Seed:", "`" + seedf + "`", false)
                                .setColor(Color.YELLOW);

                        MessageEmbed embed = embedBuilder.build();

                        if(!embedsAreEqual(embed,oldEmbed)) {
                            hook.editMessageEmbeds(embedBuilder.build()).queue();
                            hook.editMessage("Updated "+ "<t:" + (System.currentTimeMillis() / 1000) + ":R>").queue();
                            oldEmbed = embed;
                        }
                    }
                    case "Done" -> {
                        running = false;

                        String settingsString = Base64.encodeBase64String(model.getBytes()) + ":"
                                + Base64.encodeBase64String(positivePrompt.getBytes()) + ":"
                                + Base64.encodeBase64String(negativePrompt.getBytes()) + ":"
                                + Base64.encodeBase64String(basePromptPreset.getBytes()) + ":"
                                + Base64.encodeBase64String(sizeString.getBytes()) + ":"
                                + Base64.encodeBase64String(seedString.getBytes()) + ":"
                                + Base64.encodeBase64String(new String(intToByteArray(width)).getBytes()) + ":"
                                + Base64.encodeBase64String(new String(intToByteArray(height)).getBytes());
                        String newUUID = UUID.randomUUID().toString();

                        settingsMap.put(newUUID,settingsString);

                        Button regenerate = Button.primary("regenerate:" + newUUID, "Generate again");
                        Button deleteButton = Button.danger("delete", "Delete");
                        Button upscale = Button.primary("upscale", "Upscale");

                        EmbedBuilder embedBuilder = new EmbedBuilder()
                                .setTitle("Uploading...")
                                .addField("Model:           ", "`" + model + "`", false)
                                .addField("Preset:          ", "`" + (basePromptPreset.isEmpty() ? "None" : basePromptPreset) + "`", false)
                                .addField("Positive Prompt: ", "`" + positivePrompt + "`", false)
                                .addField("Negative Prompt: ", "`" + (negativePrompt.isEmpty() ? " " : negativePrompt) + "`", false)
                                .addField("Image Size:      ", "`" + widthf + " x " + heightf + "`", false)
                                .addField("Seed:", "`" + seedf + "`", false)
                                .addField("", " ", false)
                                .addField("Uploading Image...","",false)
                                .setColor(Color.GREEN);

                        hook.editMessageEmbeds(embedBuilder.build())
                                .setActionRow(regenerate, upscale, deleteButton)
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

                        String userPrompt = "Reply with ONLY 'YES' or 'NO'. No explanation. Capital letters only. Does the following prompt imply nudity, sexual content, violence, or gore: '" + positivePrompt + "'?";

                        String result = "Error";
                        try {
                            result = askModel("llama3:8b", userPrompt);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        String filename = "image_" + System.currentTimeMillis() + ".png";

                        boolean nsfw = result.equals("YES");
                        if(nsfw) filename = "SPOILER_" + filename;

                        ImageIO.write(image, "png", new File("nudes/" + filename));

                        embedBuilder = new EmbedBuilder()
                                .setTitle("Done!")
                                .addField("Model:           ", "`" + model + "`", false)
                                .addField("Preset:          ", "`" + (basePromptPreset.isEmpty() ? "None" : basePromptPreset) + "`", false)
                                .addField("Positive Prompt: ", "`" + positivePrompt + "`", false)
                                .addField("Negative Prompt: ", "`" + (negativePrompt.isEmpty() ? " " : negativePrompt) + "`", false)
                                .addField("Image Size:      ", "`" + image.getWidth() + " x " + image.getHeight() + "`", false)
                                .addField("Seed:", "`" + seedf + "`", false)
                                .setColor(Color.GREEN)
                                .setThumbnail("attachment://thumbnail.png")
                                .setImage("attachment://" + filename);

                        if(nsfw) embedBuilder.addField("NSFW","⚠️ Potentially NSFW - click to reveal",false);

                        hook.editMessageEmbeds(embedBuilder.build())
                                .setFiles(
                                        FileUpload.fromData(thumbnailData, "thumbnail.png"),
                                        FileUpload.fromData(imageData, filename)
                                )
                                .queue(sentMessage -> {
                                });

                        hook.editMessage("").queue();
                    }
                    case "Error" -> {
                        String errormsg = status.getString("error");
                        if(errormsg.length() > 1024) errormsg = errormsg.substring(0, 1024);

                        EmbedBuilder embedBuilder = new EmbedBuilder()
                                .setTitle("An Error occured.")
                                .addField("Model:", "`" + model + "`", false)
                                .addField("Preset:", "`" + (basePromptPreset.isEmpty() ? "None" : basePromptPreset) + "`", false)
                                .addField("Positive Prompt:", "`" + positivePrompt + "`", false)
                                .addField("Negative Prompt:", "`" + (negativePrompt.isEmpty() ? " " : negativePrompt) + "`", false)
                                .addField("Image Size:", "`" + widthf + " x " + heightf + "`", false)
                                .addField("Seed:", "`" + seedf + "`", false)
                                .addField("","",false)
                                .addField("Error:", errormsg, false)
                                .setColor(Color.RED);

                        hook.editMessageEmbeds(embedBuilder.build()).queue();
                        //hook.editMessage("An error occured. \n" + status.toString(3)).queue();
                        running = false;
                        return;
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            try {
                Thread.sleep(1000);
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

    public static boolean embedsAreEqual(MessageEmbed a, MessageEmbed b) {
        if (a == null || b == null) return false;
        return Objects.equals(a.getTitle(), b.getTitle()) &&
                Objects.equals(a.getDescription(), b.getDescription()) &&
                Objects.equals(a.getFields(), b.getFields()) &&
                Objects.equals(a.getFooter(), b.getFooter()) &&
                Objects.equals(a.getImage(), b.getImage()) &&
                Objects.equals(a.getThumbnail(), b.getThumbnail()) &&
                Objects.equals(a.getAuthor(), b.getAuthor()) &&
                Objects.equals(a.getColorRaw(), b.getColorRaw());
    }

    public static byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >> 24),
                (byte)(value >> 16),
                (byte)(value >> 8),
                (byte)(value)
        };
    }

    public static int byteArrayToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) |
                ((bytes[1] & 0xFF) << 16) |
                ((bytes[2] & 0xFF) << 8) |
                (bytes[3] & 0xFF);
    }

}
