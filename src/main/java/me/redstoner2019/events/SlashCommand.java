package me.redstoner2019.events;

import me.redstoner2019.Main;
import me.redstoner2019.chatgpt.Ollama;
import me.redstoner2019.tts.TTSPlayer;
import me.redstoner2019.ttsaws.TwitchTTS;
import me.redstoner2019.utils.CompressionUtility;
import me.redstoner2019.utils.LongMessageSender;
import me.redstoner2019.utils.SHA256Hash;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.FileUpload;
import org.json.JSONArray;
import org.json.JSONObject;
import software.amazon.awssdk.services.polly.model.VoiceId;


import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

import static me.redstoner2019.Main.jda;

public class SlashCommand extends ListenerAdapter {

    public static Random rng = new Random();
    public static int lastId = 0;

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String user = event.getUser().getName();
        Guild guild = event.getGuild();

        if(event.getName().equals("chat")){
            event.deferReply().queue(message -> {
                try {
                    String prompt = event.getOption("message").getAsString();
                    String response = Ollama.askModel("tinyllama:1.1b",prompt);
                    response = "Prompt: `" + prompt + "`\n\n" + response;

                    if(response.length() < 2000){
                        message.editOriginal(response).queue();
                    } else {
                        message.editOriginal(response.substring(0,2000)).queue();
                        LongMessageSender.sendLongMessage((TextChannel) event.getChannel(),response.substring(2000));
                    }
                } catch (Exception e) {
                    message.editOriginal(ChatEvent.exceptionToString(e)).queue();
                }
            });
        }

        if(event.getName().equals("vc")){
            if(event.getOption("speed") != null) {
                int speed = event.getOption("speed").getAsInt();
                if(speed < 10 || speed > 1000){
                    event.reply("Speed must be between 10 and 1000").queue();
                } else {
                    Main.nameSpeed.put(user, (float) speed);
                    event.reply("Successfully updated speed to " + speed + "%").queue();
                }
                return;
            }
            if(event.getOption("pitch") != null) {
                int pitch = event.getOption("pitch").getAsInt();
                if(pitch < -100 || pitch > 100){
                    event.reply("Speed must be between -100 and 100").queue();
                } else {
                    Main.namePitch.put(user, (float) pitch);
                    event.reply("Successfully updated pitch to " + pitch + "%").queue();
                }
                return;
            }
            if(event.getOption("alias") != null) {
                String alias = event.getOption("alias").getAsString();
                Main.nameAlias.put(user, alias);
                event.reply("Successfully updated your alias to " + alias + "!").queue();
                return;
            }
            if(event.getOption("voice") != null) {
                String voice = event.getOption("voice").getAsString();
                try{
                    Main.nameVoices.put(user,VoiceId.valueOf(voice.toUpperCase()));
                    event.reply("Your voice was updated to " + Main.nameVoices.get(user)).queue();
                }catch (Exception e){
                    event.reply("This voice was not found. " + voice).queue();
                }
                return;
            }
            if(event.getOption("say") != null) {
                String message = event.getOption("say").getAsString();

                GuildVoiceState voiceState = event.getMember().getVoiceState();
                if (voiceState == null || !voiceState.inAudioChannel()) {
                    event.reply("You are not currently in a voice channel!").queue();
                    return;
                }

                VoiceChannel channel = voiceState.getChannel().asVoiceChannel();

                try {
                    String name = UUID.randomUUID().toString();
                    TwitchTTS.generateTTS(message, name,
                            Main.nameVoices.getOrDefault(user, VoiceId.IVY),
                            Main.nameSpeed.getOrDefault(user, 100f),
                            Main.namePitch.getOrDefault(user, 0f)
                    );

                    TTSPlayer player = new TTSPlayer(guild);
                    player.playFile(channel, name);

                    event.reply("Success").queue();
                } catch (Exception e) {
                    event.reply("An error occured: " + e.getMessage()).queue();
                    e.printStackTrace();
                }
                return;
            }
            if(!event.isAcknowledged()) event.reply("Nothing selected!").queue();
        }

        if(event.getName().equals("youtube")) {
            GuildVoiceState voiceState = event.getMember().getVoiceState();
            if (voiceState == null || !voiceState.inAudioChannel()) {
                event.reply("You are not currently in a voice channel!").queue();
                return;
            }

            VoiceChannel channel = voiceState.getChannel().asVoiceChannel();

            event.reply("Starting...").queue(message -> {
                TTSPlayer player = new TTSPlayer(guild);
                player.playYoutube(channel, event.getOption("url").getAsString(), message);
            });
        }
        if(event.getName().equals("sfx")){
            GuildVoiceState voiceState = event.getMember().getVoiceState();
            if (voiceState == null || !voiceState.inAudioChannel()) {
                event.reply("You are not currently in a voice channel!").queue();
                return;
            }

            VoiceChannel channel = voiceState.getChannel().asVoiceChannel();

            if(event.getOption("sound") != null) {
                String sound = event.getOption("sound").getAsString();
                if(Main.sfx.containsKey(sound)){
                    TTSPlayer player = new TTSPlayer(guild);
                    player.playFile(channel, Main.sfx.get(sound), false);
                    event.reply("SFX `" + sound + "` is now playing!").queue();
                    TextChannel tc = jda.getTextChannelById("1353370006892838983");
                    if(tc != null) {
                        EmbedBuilder eb = new EmbedBuilder();
                        eb.setColor(Color.RED);
                        eb.setTitle("Playing SFX: **" + event.getOption("sound").getAsString() + "** by `" + event.getMember().getUser().getName() + "`");

                        String connected = "";

                        for(Member member : channel.getMembers()){
                            if(member.getUser().isBot()) continue;
                            connected += "\n `" + member.getEffectiveName() + " (" + member.getUser().getName() + ")`";
                        }

                        eb.addField("In: " + channel.getJumpUrl() + " (" + guild.getName() + ")","To: " + connected,false);

                        tc.sendMessageEmbeds(eb.build()).queue();
                    } else {
                        System.out.println("null");
                    }
                } else {
                    event.reply("Sound  " + sound + " nicht gefunden!").queue();
                }
                return;
            }
            return;
        }
        if(event.getName().equals("togglevcreading")) {
            Main.vcreading.put(event.getGuild().getId(),!Main.vcreading.getOrDefault(event.getGuild().getId(),true));
            event.reply("Toggled VC-Reading to " + Main.vcreading.getOrDefault(event.getGuild().getId(),true)).queue();
        }
        if(event.getName().equals("toggleprefixreading")) {
            Main.prefixReading.put(event.getGuild().getId(),!Main.prefixReading.getOrDefault(event.getGuild().getId(),true));
            event.reply("Toggled VC-Prefix-Reading to " + Main.prefixReading.getOrDefault(event.getGuild().getId(),true)).queue();
        }
        if(event.getName().equals("zitat")) {
            List<String> channels = Main.randomMessageChannel.get(event.getGuild().getId());
            String channel = channels.get(new Random().nextInt(channels.size()));

            TextChannel tc = guild.getTextChannelById(channel);

            String id = Main.messageIndexer.get(channel).get(new Random().nextInt(Main.messageIndexer.get(channel).size()));
            tc.retrieveMessageById(id).queue(message1 -> {
                try{
                    GuildVoiceState voiceState = event.getMember().getVoiceState();
                    if (voiceState == null || !voiceState.inAudioChannel()) {
                        event.reply("You are not currently in a voice channel!").queue();
                    } else {
                        VoiceChannel voiceChannel = voiceState.getChannel().asVoiceChannel();

                        String messageText = message1.getContentRaw();

                        List<User> mentionedMembers = message1.getMentions().getUsers();

                        for (User member : mentionedMembers) {

                            String name = member.getEffectiveName();
                            if(Main.nameAlias.containsKey(member.getName())) name = Main.nameAlias.get(member.getName());

                            String mentionTag = "<@"+member.getId()+">";
                            String mentionTagWithNick = "<@!"+member.getId()+">";

                            messageText = messageText.replace(mentionTag, name);
                            messageText = messageText.replace(mentionTagWithNick, name);
                        }

                        String name = UUID.randomUUID().toString();
                        TwitchTTS.generateTTS(messageText.replaceAll("\n","  -  "), name,
                                VoiceId.HANS,
                                100f,
                                0f
                        );

                        event.reply(message1.getTimeCreated().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n" + message1.getJumpUrl() + " - by " + message1.getAuthor().getName() + "\n" + messageText).queue();

                        TTSPlayer player = new TTSPlayer(guild);
                        player.playFile(voiceChannel,name);
                    }
                }catch (Exception e){
                    event.reply("An error occured: " + e.getMessage()).queue();
                }
            });
        }

        if(event.getName().equals("uploadsfx")){
            String sfxName = event.getOption("sfxname").getAsString();
            Message.Attachment file = event.getOption("file").getAsAttachment();

            String fileName = file.getFileName().toLowerCase();
            String contentType = file.getContentType();

            System.out.println(contentType + " " + sfxName + " " + fileName);

            if (!(fileName.endsWith(".mp3") || fileName.endsWith(".wav") || fileName.endsWith(".ogg") || fileName.endsWith(".m4a"))) {
                event.reply("Only Audio-Files (.mp3, .wav, .ogg) are accepted!").setEphemeral(true).queue();
                return;
            }

            if(Main.sfx.containsKey(sfxName)){
                event.reply("This SFX-Name is already in use!").setEphemeral(true).queue();
                return;
            }

            try {
                BufferedInputStream in = new BufferedInputStream(new URL(file.getUrl()).openStream());

                String originalName = fileName + "";

                if(new File(fileName).exists()) {
                    fileName+="-" + UUID.randomUUID();
                }

                FileOutputStream fos = new FileOutputStream(fileName);
                fos.write(in.readAllBytes());

                fos.close();
                in.close();

                FileInputStream fis = new FileInputStream("sfx.json");
                JSONObject sfxobj = new JSONObject(new String(fis.readAllBytes()));
                fis.close();
                JSONArray sfxArray = sfxobj.getJSONArray("sfx");

                JSONObject fileObj = new JSONObject();
                fileObj.put("alias",sfxName);
                fileObj.put("file",fileName);
                fileObj.put("by", event.getUser().getName());
                fileObj.put("at", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                sfxArray.put(fileObj);

                fos = new FileOutputStream("sfx.json");
                fos.write(sfxobj.toString(3).getBytes());
                fos.close();

                event.reply("File **" + originalName + "** uploaded successfully!").queue();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if(event.getName().equals("nude")){
            /*if(!guild.getId().equals("1143990894589128745")){
                event.reply("This command is still in development.").queue();
                return;
            }*/

            MessageChannel channel = event.getChannel();

            if (channel instanceof TextChannel textChannel) {
                if (!textChannel.isNSFW() || textChannel.getId().equals("1108922674585018459")) {
                    event.reply("🔞 This command is only allowed in NSFW channels.").queue();
                    return;
                }
            }

            if(Main.nudesArray.isEmpty()){
                event.reply("There is nothing uploaded yet.").queue();
                return;
            }

            int i = rng.nextInt(Main.nudesArray.length());

            while(i == lastId){
                i = rng.nextInt(Main.nudesArray.length());
            }
            lastId = i;

            JSONObject randomInfo = Main.nudesArray.getJSONObject(i);

            String originalName = randomInfo.getString("original-file-name");

            ByteArrayInputStream stream;
            try {
                stream = CompressionUtility.decompressFileAsStream(new File("nudes/" + randomInfo.getString("uuid")));
            } catch (IOException e) {
                event.reply("An error occured. Code: 101\n\n" + ChatEvent.exceptionToString(e)).queue();
                return;
            }

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("No touchie :face_with_hand_over_mouth: ");
            embed.setColor(Color.ORANGE);
            embed.setImage("attachment://" + originalName);
            event.replyEmbeds(embed.build())
                    .addFiles(FileUpload.fromData(stream,originalName))
                    .queue();
        }

        if(event.getName().equals("uploadnude")){
            /*if(!guild.getId().equals("1143990894589128745")){
                event.reply("This command is still in development.").queue();
                return;
            }*/

            Message.Attachment file = event.getOption("file").getAsAttachment();

            MessageChannel channel = event.getChannel();

            File nudesBase = new File("nudes");
            if(!nudesBase.exists()) {
                if(!nudesBase.mkdir()) {
                    event.reply("An error occured. Code: 101").queue();
                    return;
                }
            }
            File nudesIndexFile = new File("nudes/index.json");
            if(!nudesIndexFile.exists()) {
                try {
                    if(!nudesIndexFile.createNewFile()) {
                        event.reply("An error occured. Code: 102").queue();
                        return;
                    }
                } catch (IOException e) {
                    event.reply("An error occured. Code: 102").queue();
                    return;
                }
            }

            String originalName = file.getFileName();

            if(file.getSize() >= 26214400) {
                event.reply("This file is too large. Max accepted is 25 MB.").setEphemeral(true).queue();
                return;
            }

            if (!(originalName.endsWith(".png") || originalName.endsWith(".jpg") || originalName.endsWith(".jpeg") || originalName.endsWith(".webp") || originalName.endsWith(".gif"))) {
                event.reply("Only Image-Files are accepted! If you believe the files is an image file and should be supported, please contact the server admins.").setEphemeral(true).queue();
                return;
            }

            JSONObject info = new JSONObject();
            info.put("guild",guild.getId());
            info.put("name",guild.getName());
            info.put("uploader-id",event.getUser().getId());
            info.put("uploader-tag",event.getUser().getAsTag());
            info.put("uploader-name",event.getUser().getName());
            info.put("time",LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            info.put("original-file-name",originalName);

            try {
                BufferedInputStream in = new BufferedInputStream(new URL(file.getUrl()).openStream());

                FileOutputStream fos = new FileOutputStream("nudes/temp");
                fos.write(in.readAllBytes());

                fos.close();
                in.close();
                info.put("uuid", SHA256Hash.hashFile("nudes/temp"));

                CompressionUtility.compressFile(new File("nudes/temp"),new File("nudes/" + info.getString("uuid")), 22);

                Main.nudesArray.put(info);

                fos = new FileOutputStream("nudes/index.json");
                fos.write(Main.nudesArray.toString(3).getBytes(StandardCharsets.UTF_8));
                fos.close();

                if (channel instanceof TextChannel textChannel) {
                    if (!textChannel.isNSFW()) {
                        event.reply("🔞 This command is only allowed in NSFW channels.").queue();
                        return;
                    }
                }

                event.reply("Uploaded " + originalName + " successfully.").queue();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (event.getName().equals("vc") && event.getFocusedOption().getName().equals("voice")) {
            List<VoiceId> suggestions = List.of(VoiceId.values());

            List<VoiceId> filteredSuggestions = suggestions.stream()
                    .filter(s -> s.name().toLowerCase().startsWith(event.getFocusedOption().getValue().toLowerCase()))
                    .limit(25)
                    .toList();

            event.replyChoices(filteredSuggestions.stream()
                    .map(choice -> new net.dv8tion.jda.api.interactions.commands.Command.Choice(choice.name(), choice.name()))
                    .toList()
            ).queue();
        }

        if (event.getName().equals("sfx") && event.getFocusedOption().getName().equals("sound")) {
            List<String> suggestions = new ArrayList<>(Main.sfx.keySet());

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
}
