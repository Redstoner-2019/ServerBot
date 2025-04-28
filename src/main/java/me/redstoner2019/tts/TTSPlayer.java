package me.redstoner2019.tts;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import me.redstoner2019.Main;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.managers.AudioManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class TTSPlayer {
    private final AudioPlayerManager playerManager;
    private final AudioPlayer player;
    private final Guild guild;

    public TTSPlayer(Guild guild) {
        this.guild = guild;
        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);

        this.player = playerManager.createPlayer();
    }

    public void playBytes(VoiceChannel voiceChannel, byte[] bytes){
        AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
    }

    public void playFile(VoiceChannel voiceChannel, String filename) {
        playFile(voiceChannel, filename, true);
    }

    public void playFile(VoiceChannel voiceChannel, String filename, boolean delete){
        guild.getAudioManager().setSendingHandler(new AudioPlayerSendHandler(player));
        AudioManager audioManager = guild.getAudioManager();
        audioManager.openAudioConnection(voiceChannel);

        System.out.println("Putting timestamp");
        Main.lastReadTimestamp.put(voiceChannel, LocalDateTime.now());

        playerManager.loadItem(filename, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                player.startTrack(track, false);
                if(delete){
                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(track.getDuration() + 10000);
                                new File(filename).delete();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                    t.start();
                }
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                System.out.println("Playlist loaded, but we are not using playlists.");
            }

            @Override
            public void noMatches() {
                System.out.println("No audio file found!");
            }

            @Override
            public void loadFailed(FriendlyException e) {
                System.out.println(e.getMessage());
            }
        });
    }

    public void playYoutube(VoiceChannel voiceChannel, String link, InteractionHook message){
        System.out.println("Starting youtube playback");

        String uuid = UUID.randomUUID().toString();

        try{
            List<String> command = Arrays.asList(
                    "yt-dlp",
                    "-x",                         // extract audio
                    "--cookies", "cookies.txt",
                    "--audio-format", "mp3",
                    "--audio-quality", "0",       // best quality
                    "-o","youtube/" + uuid + ".mp3",
                    link
            );

            System.out.println(link);

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            int exitCode = process.waitFor();
            System.out.println("Download finished with exit code " + exitCode);
            if(message != null) message.editOriginal("Done! ✅ \nExit code " + exitCode).queue();
            if(exitCode == 0){
                playFile(voiceChannel, "youtube/" + uuid + ".mp3");
            }

            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000*60*10);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    new File("youtube/" + uuid + ".mp3").delete();
                }
            });
            t.start();
        }catch (Exception e) {

        }
    }

    public void joinAndPlayTTS(VoiceChannel voiceChannel, String text) {
        guild.getAudioManager().setSendingHandler(new AudioPlayerSendHandler(player));
        try {
            System.out.println("Generating...");
            // Step 1: Generate TTS using Python
            String audioFile = TTSGenerator.generateTTS(text);
            if (audioFile == null) {
                System.out.println("TTS generation failed.");
                return;
            }

            System.out.println("Joining...");

            // Step 2: Join Voice Channel
            AudioManager audioManager = guild.getAudioManager();
            audioManager.openAudioConnection(voiceChannel);

            System.out.println("Playing...");
            System.out.println(audioFile);
            // Step 3: Load and Play TTS Audio
            playerManager.loadItem(audioFile, new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack track) {
                    System.out.println("Start track");
                    player.startTrack(track, false);
                    System.out.println("End track");
                    //audioManager.closeAudioConnection();
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                    System.out.println("Playlist loaded, but we are not using playlists.");
                }

                @Override
                public void noMatches() {
                    System.out.println("No audio file found!");
                }

                @Override
                public void loadFailed(FriendlyException e) {
                    System.out.println(e.getMessage());
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
