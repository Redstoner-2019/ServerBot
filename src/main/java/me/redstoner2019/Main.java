package me.redstoner2019;

import com.neovisionaries.ws.client.WebSocketFactory;
import me.redstoner2019.chatgpt.StableDiffusion;
import me.redstoner2019.events.ChatEvent;
import me.redstoner2019.events.JoinEvent;
import me.redstoner2019.events.SDSlashCommand;
import me.redstoner2019.events.SlashCommand;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import okhttp3.OkHttpClient;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import software.amazon.awssdk.services.polly.model.VoiceId;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Main {
    public static JDA jda;
    public static String prefix = "vc ";

    public static HashMap<String, List<String>> messageIndexer = new HashMap<>();
    public static HashMap<String, List<String>> randomMessageChannel = new HashMap<>();
    public static HashMap<String, String> nameAlias = new HashMap<>();
    public static HashMap<String, VoiceId> nameVoices = new HashMap<>();
    public static HashMap<String, Float> nameSpeed = new HashMap<>();
    public static HashMap<String, Float> namePitch = new HashMap<>();
    public static HashMap<String, String> sfx = new HashMap<>();
    public static HashMap<String, Boolean> vcreading = new HashMap<>();
    public static HashMap<String, Boolean> prefixReading = new HashMap<>();
    public static HashMap<VoiceChannel, LocalDateTime> lastReadTimestamp = new HashMap<>();
    public static boolean TEST = true;
    public static boolean GPU_DETECTED = false;
    /**
     * Nudes Stuff
     */
    public static JSONArray nudesArray = new JSONArray();

    public static void main(String[] args) {
        String TOKEN = new String(Base64.decodeBase64("TVRNME5EQTJOVGt3TnpVeE5qZzVPVFEwTWcuR2NqTmJSLlFSUDAwdHBHbElxaF80QVVycmVEc3FvenltanpWLUYweXNUamNz"));
        if(TEST) TOKEN = new String(Base64.decodeBase64("TVRNMk5UYzNNRFl3TkRZNU9Ea3lOekl5TlEuR2d1NWkyLmJnU0Vrb1VaNlgxLUROQTY0LTg1Q0tITnVoSWswcXMzVklMOUVN"));

        OkHttpClient unsafeClient = getUnsafeOkHttpClient();
        WebSocketFactory unsafeFactory = getUnsafeWebSocketFactory();

        jda = JDABuilder
                .createDefault(TOKEN).enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .setHttpClientBuilder(unsafeClient.newBuilder())
                .setWebsocketFactory(unsafeFactory)
                .build();

        jda.getPresence().setPresence(OnlineStatus.ONLINE,Activity.customStatus("Being a stone"));
        jda.addEventListener(new ChatEvent());
        jda.addEventListener(new SlashCommand());
        jda.addEventListener(new JoinEvent());
        jda.addEventListener(new SDSlashCommand());

        StableDiffusion.startScheduler();

        List<String> statusses = new ArrayList<>();
        statusses.add("Feeling Catly Today✨\uD83D\uDC85");
        statusses.add("Cracking the reality");
        statusses.add("Cracking the cracking crack out of meth");
        statusses.add("Cracktastic!✨");

        GPU_DETECTED = !GraphicsEnvironment.isHeadless();

        /*sfx.put("uwu","uwu.mp3");
        sfx.put("happi","happy-happy-happy-song.mp3");
        sfx.put("chipi","chipi-chipi-chapa-chapa.mp3");
        sfx.put("ahh","ahh.mp3");
        sfx.put("sus","sus.mp3");
        sfx.put("cri","cri.mp3");
        sfx.put("dasgehtdochnicht","dasgehtdochnicht.mp3");
        sfx.put("amogus","amogus.mp3");
        sfx.put("dumdumdumdudidui","dumdumdumdudidui.mp3");
        sfx.put("thomas","thomas.mp3");
        sfx.put("pornhub","pornhub.mp3");
        sfx.put("itsame","itsame.mp3");
        sfx.put("mariobedoo","mariobedoo.mp3");
        sfx.put("leave","leave.mp3");*/
        try {
            FileInputStream fis = new FileInputStream("save.json");
            JSONObject obj = new JSONObject(new String(fis.readAllBytes()));
            fis.close();

            JSONObject users = obj.getJSONObject("users");
            JSONObject channels = obj.getJSONObject("channels");

            for(String user : users.keySet()){
                JSONObject userObj = users.getJSONObject(user);
                nameAlias.put(user, userObj.getString("alias"));
                nameVoices.put(user, VoiceId.valueOf(userObj.getString("voice")));
                nameSpeed.put(user, userObj.getFloat("speed"));
                namePitch.put(user, userObj.getFloat("pitch"));
            }

            for(String guild : channels.keySet()){
                List<String> channelsList = new ArrayList<>();
                JSONArray channelsArray = channels.getJSONArray(guild);
                for(int i = 0; i < channelsArray.length(); i++){
                    channelsList.add(channelsArray.getString(i));
                }
                randomMessageChannel.put(guild, channelsList);
            }

            fis = new FileInputStream("sfx.json");
            JSONObject sfxobj = new JSONObject(new String(fis.readAllBytes()));
            fis.close();
            JSONArray sfxArray = sfxobj.getJSONArray("sfx");
            for (int i = 0; i < sfxArray.length(); i++) {
                JSONObject sfxo = sfxArray.getJSONObject(i);
                sfx.put(sfxo.getString("alias"), sfxo.getString("file"));
            }

            for(Guild g : jda.getGuilds()){
                if(!randomMessageChannel.containsKey(g.getId())) randomMessageChannel.put(g.getId(), new ArrayList<>());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                for(Guild g : jda.getGuilds()){
                    if(false) break;
                    System.out.println("Updating for " + g);
                    g.updateCommands().queue();
                    g.updateCommands().addCommands(
                            Commands.slash("vc","The vc command!")
                                    .addOption(OptionType.INTEGER, "speed", "Speed of the tts!", false)
                                    .addOption(OptionType.INTEGER, "pitch", "Pitch of the tts!", false)
                                    .addOption(OptionType.STRING, "alias", "Alias when reading your message!", false)
                                    .addOption(OptionType.STRING, "voice", "Set your voice!", false,true)
                                    .addOption(OptionType.STRING, "say", "Say something in VC!", false),
                            Commands.slash("youtube","Play audio of a youtube video!")
                                    .addOption(OptionType.STRING, "url", "The URL!", true),
                            Commands.slash("zitat","Sucht ein random zitat!"),
                            Commands.slash("sfx","Spiel einen SFX ab!")
                                    .addOption(OptionType.STRING, "sound", "Der sound!", true,true),
                            Commands.slash("uploadsfx","Upload an SFX!")
                                    .addOption(OptionType.STRING, "sfxname", "sfxname", true)
                                    .addOption(OptionType.ATTACHMENT, "file", "Sound File", true),
                            Commands.slash("voices","Liste alle Stimmen auf!"),
                            Commands.slash("togglevcreading","Toggles automatic reading in voice channels"),
                            Commands.slash("toggleprefixreading","Toggles reading of the prefix in voice channels"),
                            Commands.slash("uploadnude","Upload a nude (You are naughty)")
                                    .addOption(OptionType.ATTACHMENT, "file", "The nude", true),
                            Commands.slash("nude","Show a random nude (You are naughty)"),
                            Commands.slash("chat","Ask the AI model something!")
                                    .addOption(OptionType.STRING,"message","Your Prompt",true)
                                    .addOption(OptionType.STRING,"model","The LLM",false, true),
                            Commands.slash("generateimage","AI Generate an Image!")
                                    .addOption(OptionType.STRING,"model","The generation Model", true, true)
                                    .addOption(OptionType.STRING,"positiveprompt","Positive Prompt (This is what you want the AI to do)", true)
                                    .addOption(OptionType.STRING,"negativeprompt","Negative Prompt (This is what you DONT want the AI to do)", false)
                                    .addOption(OptionType.STRING,"basepromtpreset","Base Promt Preset, allows you to not have to type in all the default prompts", false, true)
                                    .addOption(OptionType.STRING,"size","Image Size (LARGE = 1024x1024, MEDIUM = 512x512, SMALL = 256x256)", false, true)
                                    .addOption(OptionType.INTEGER,"width","Width (128 < 2048)", false)
                                    .addOption(OptionType.INTEGER,"height","Height (128 < 2048)", false)
                                    .addOption(OptionType.STRING,"seed","Seed", false)
                                    .addOption(OptionType.NUMBER,"age","Controls the age. Input number between 0 and 1. ⚠️ NOT GUARANTEED TO WORK WITH EVERY MODEL! ⚠️", false)
                    ).queue();
                }

                indexAllMessages();

                while (true) {
                    try {
                        Thread.sleep(5000);
                        FileInputStream fis = new FileInputStream("sfx.json");
                        JSONObject sfxobj = new JSONObject(new String(fis.readAllBytes()));
                        fis.close();
                        JSONArray sfxArray = sfxobj.getJSONArray("sfx");
                        for (int i = 0; i < sfxArray.length(); i++) {
                            JSONObject sfxo = sfxArray.getJSONObject(i);
                            sfx.put(sfxo.getString("alias"), sfxo.getString("file"));
                        }

                        if(new File("nudes/index.json").exists()) {
                            fis = new FileInputStream("nudes/index.json");
                            nudesArray = new JSONArray(new String(fis.readAllBytes()));
                            fis.close();
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    for(VoiceChannel v : lastReadTimestamp.keySet()){
                        if(lastReadTimestamp.get(v).plusMinutes(30).isBefore(LocalDateTime.now())){
                            Guild g = v.getGuild();
                            AudioManager am = g.getAudioManager();
                            am.closeAudioConnection();

                            lastReadTimestamp.remove(v);
                        }
                    }

                    jda.getPresence().setPresence(OnlineStatus.ONLINE,Activity.customStatus(statusses.get(new Random().nextInt(statusses.size()-1))));

                    List<String> names = new ArrayList<>();
                    for(String s : nameAlias.keySet()) if(!names.contains(s)) names.add(s);
                    for(String s : nameVoices.keySet()) if(!names.contains(s)) names.add(s);
                    for(String s : nameSpeed.keySet()) if(!names.contains(s)) names.add(s);
                    for(String s : namePitch.keySet()) if(!names.contains(s)) names.add(s);

                    JSONObject json = new JSONObject();
                    for(String user : names){
                        JSONObject userObj = new JSONObject();
                        userObj.put("alias", nameAlias.getOrDefault(user,user));
                        userObj.put("voice", nameVoices.getOrDefault(user,VoiceId.IVY).name());
                        userObj.put("speed", nameSpeed.getOrDefault(user,100f));
                        userObj.put("pitch", namePitch.getOrDefault(user,0f));
                        json.put(user, userObj);
                    }

                    JSONObject channels = new JSONObject();
                    for(String guild : randomMessageChannel.keySet()) {
                        JSONArray jsonArray = new JSONArray();
                        for(String channel : randomMessageChannel.get(guild)) jsonArray.put(channel);
                        channels.put(guild, jsonArray);
                    }

                    JSONObject global = new JSONObject();
                    global.put("users",json);
                    global.put("channels",channels);

                    try {
                        FileOutputStream fos = new FileOutputStream("save.json");
                        fos.write(global.toString(3).getBytes());
                        fos.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        t.start();
    }

    public static void indexAllMessages(){
        System.out.println("Indexing all messages");
        System.out.println(jda.getGuilds().size());
        for(Guild guild : jda.getGuilds()){
            System.out.println(guild.getName());
            System.out.println("Indexing guild " + guild.getId() + " - " + guild.getName());
            if(!randomMessageChannel.containsKey(guild.getId())) continue;
            for(String channel : randomMessageChannel.get(guild.getId())){
                TextChannel tc = guild.getTextChannelById(channel);
                System.out.println("indexing channel " + channel);
                indexMessages(tc);
            }
        }
        System.out.println("Done");
    }

    public static void indexMessages(TextChannel channel) {
        if (channel == null) {
            System.out.println("Channel not found!");
            return;
        }

        messageIndexer.put(channel.getId(), new ArrayList<>());

        fetchMessagesRecursively(channel, channel.getLatestMessageId());

        System.out.println("Completed indexing " + channel.getId());
    }

    private static void fetchMessagesRecursively(TextChannel channel, String lastMessageId) {
        channel.getHistoryBefore(lastMessageId,100).queue(messages -> {
            if (messages.isEmpty()) {
                System.out.println(channel.getName() + " -> Found " + messageIndexer.get(channel.getId()).size());
                return;
            }

            String newLastMessageId = messages.getRetrievedHistory().get(messages.size() - 1).getId();

            for(Message msg : messages.getRetrievedHistory()){
                if(messageIndexer.get(channel.getId()).contains(msg.getId())) return;
                String messageContent = msg.getContentRaw();
                if(messageContent.contains("\"")){
                    messageIndexer.get(channel.getId()).add(msg.getId());
                }
            }

            fetchMessagesRecursively(channel, newLastMessageId);
        });
    }

    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static WebSocketFactory getUnsafeWebSocketFactory() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            WebSocketFactory factory = new WebSocketFactory();
            factory.setSSLContext(sslContext);
            factory.setVerifyHostname(false); // GANZ WICHTIG!

            return factory;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}