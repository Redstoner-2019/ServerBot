package me.redstoner2019.events;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.redstoner2019.Main;
import me.redstoner2019.tts.AudioPlayerSendHandler;
import me.redstoner2019.tts.TTSPlayer;
import me.redstoner2019.ttsaws.TwitchTTS;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import software.amazon.awssdk.services.polly.model.Engine;
import software.amazon.awssdk.services.polly.model.VoiceId;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static me.redstoner2019.Main.jda;

public class ChatEvent extends ListenerAdapter {
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        try{
            if(event.getAuthor().isBot()) return;
            boolean isVoice = event.getChannel() instanceof VoiceChannel;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    if(Main.messageIndexer.containsKey(event.getChannel().getId())) Main.indexMessages(event.getChannel().asTextChannel());
                }
            }).start();

            String message = event.getMessage().getContentRaw();
            String user = event.getAuthor().getName();

            if(event.getMessage().getContentRaw().startsWith(Main.prefix)){
                message = message.substring(Main.prefix.length());
                String[] args = message.split(" ");
                if(args[0].equalsIgnoreCase("alias")){
                    Main.nameAlias.put(user,args[1]);
                    response(event.getMessage(),"Your alias was updated to " + Main.nameAlias.get(user));
                    return;
                }
                if(args[0].equalsIgnoreCase("setvoice")){
                    try{
                        Main.nameVoices.put(user,VoiceId.valueOf(args[1].toUpperCase()));
                        response(event.getMessage(),"Your voice was updated to " + Main.nameVoices.get(user));
                    }catch (Exception e){
                        response(event.getMessage(),"This voice was not found. " + args[1]);
                    }
                    return;
                }
                if(args[0].equalsIgnoreCase("setspeed")){
                    try{
                        float speed = Float.parseFloat(args[1]);
                        if(!(speed > 10 && speed < 1000)){
                            response(event.getMessage(),"Speed has to be between 10 and 1000.");
                            return;
                        }
                        Main.nameSpeed.put(user,speed);
                        response(event.getMessage(),"Your voice speed was updated to " + Main.nameSpeed.get(user));
                    }catch (Exception e){
                        response(event.getMessage(),"Invalid voice speed.");
                    }
                    return;
                }
                if(args[0].equalsIgnoreCase("setpitch")){
                    try{
                        float pitch = Float.parseFloat(args[1]);
                        if(!(pitch < 100 && pitch > -100)){
                            response(event.getMessage(),"Pitch has to be between -100 and 100.");
                            return;
                        }
                        Main.namePitch.put(user,pitch);
                        response(event.getMessage(),"Your voice pitch was updated to " + Main.namePitch.get(user));
                    }catch (Exception e){
                        response(event.getMessage(),"Invalid voice pitch.");
                    }
                    return;
                }
                if(args[0].equalsIgnoreCase("addChannel")){
                    MessageChannelUnion channel = event.getChannel();
                    if(Main.randomMessageChannel.getOrDefault(event.getGuild().getId(), new ArrayList<>()).contains(channel.getId())) return;
                    if(!Main.randomMessageChannel.containsKey(event.getGuild().getId())) Main.randomMessageChannel.put(event.getGuild().getId(),new ArrayList<>());
                    Main.randomMessageChannel.get(event.getGuild().getId()).add(channel.getId());

                    response(event.getMessage(),"This channel was added.");

                    Main.indexMessages(event.getChannel().asTextChannel());
                    return;
                }
                if(args[0].equalsIgnoreCase("zitat")){
                    List<String> channels = Main.randomMessageChannel.get(event.getGuild().getId());
                    String channel = channels.get(new Random().nextInt(channels.size()));

                    Guild guild = event.getGuild();
                    event.getMessage().delete().queue();

                    TextChannel tc = guild.getTextChannelById(channel);

                    String id = Main.messageIndexer.get(channel).get(new Random().nextInt(Main.messageIndexer.get(channel).size()));
                    tc.retrieveMessageById(id).queue(message1 -> {
                        try{
                            GuildVoiceState voiceState = event.getMember().getVoiceState();
                            if (voiceState == null || !voiceState.inAudioChannel()) {
                                response(event.getMessage(),"You are not currently in a voice channel!");
                                return;
                            } else {
                                VoiceChannel voiceChannel = voiceState.getChannel().asVoiceChannel();

                                String name = UUID.randomUUID().toString();
                                TwitchTTS.generateTTS(message1.getContentRaw(),name,
                                        VoiceId.HANS,
                                        100f,
                                        0f
                                );

                                response(event.getMessage(),message1.getTimeCreated().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n" + message1.getJumpUrl() + " - by " + message1.getAuthor().getName() + "\n" + message1.getContentRaw());

                                TTSPlayer player = new TTSPlayer(guild);
                                player.playFile(voiceChannel,name);
                            }
                        }catch (Exception e){
                            response(event.getMessage(),"An error occured: " + e.getMessage());
                        }
                    });
                    return;
                }
                if(args[0].equalsIgnoreCase("voices")){
                    String voices = "Voices: \n\n";
                    for(VoiceId v : VoiceId.values()){
                        voices += "- " + v.toString() + "\n";
                    }
                    response(event.getMessage(),voices);
                    return;
                }
                if(args[0].equalsIgnoreCase("youtube")){
                    Guild guild = event.getGuild();
                    VoiceChannel channel = guild.getVoiceChannelById("1143990896145203344");

                    String youtubeUrl = ""; // Example URL

                    TTSPlayer player = new TTSPlayer(guild);
                    //player.playYoutube(channel,youtubeUrl, null);
                    return;
                }
            }

            if(event.getMessage().getContentRaw().startsWith("vc ") || isVoice) {
                Guild guild = event.getGuild();

                if(!Main.vcreading.getOrDefault(guild.getId(),true)) return;

                VoiceChannel channel;

                GuildVoiceState voiceState = event.getMember().getVoiceState();
                if (voiceState == null || !voiceState.inAudioChannel()) {
                    return;
                } else {
                    channel = voiceState.getChannel().asVoiceChannel();
                }

                String msg = event.getMessage().getContentRaw();

                msg = msg.replaceAll("<a?:\\w+?:\\d+>", "");

                if(msg.isEmpty() || !event.getMessage().getEmbeds().isEmpty() || msg.isBlank()) return;

                if (!isVoice) msg = event.getMessage().getContentRaw().substring(3);

                if(Main.prefixReading.getOrDefault(guild.getId(),true)) msg = (Main.nameAlias.getOrDefault(user, user)) + " says " + msg;

                try {
                    String name = UUID.randomUUID().toString();
                    TwitchTTS.generateTTS(msg, name,
                            Main.nameVoices.getOrDefault(user, VoiceId.IVY),
                            Main.nameSpeed.getOrDefault(user, 100f),
                            Main.namePitch.getOrDefault(user, 0f)
                    );

                    TTSPlayer player = new TTSPlayer(guild);
                    player.playFile(channel, name);
                } catch (Exception e) {
                    response(event.getMessage(),"An error occured: " + e.getMessage() + "\n\nStacktrace:\n" + exceptionToString(e));
                    e.printStackTrace();
                }
            }
        }catch (Exception e){
            response(event.getMessage(),"An error occured: " + e.getMessage() + "\n\nStacktrace:\n" + exceptionToString(e));
            e.printStackTrace();
        }
    }

    public static void response(Message originalMessage, String response){
        MessageCreateAction act = originalMessage.reply(response);
        act.queue();
        //originalMessage.delete().queue();
    }

    public static String exceptionToString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
