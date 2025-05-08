package me.redstoner2019.events;

import me.redstoner2019.utils.Setting;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static me.redstoner2019.utils.Setting.*;
import static me.redstoner2019.events.SettingType.*;

public class SettingsSlashCommand extends ListenerAdapter {

    public static final HashMap<Setting, String[]> settingValues = new HashMap<>();
    public static final HashMap<Setting, String> settingDefaultValues = new HashMap<>();
    public static final HashMap<Setting, SettingType> settingsType = new HashMap<>();
    public static final HashMap<Setting, PreviewType> previewType = new HashMap<>();

    static {
        settingValues.put(STABLE_DIFFUSION_ADDRESS, new String[]{"127.0.0.1:7860","redstonerdev.io:7860"});
        previewType.put(STABLE_DIFFUSION_ADDRESS,PreviewType.STRING_LIST);
        settingDefaultValues.put(STABLE_DIFFUSION_ADDRESS,"127.0.0.1:7860");

        settingValues.put(NSFW_IN_NSFW_CHANNELS_FORCED, new String[]{"true","false"});
        previewType.put(NSFW_IN_NSFW_CHANNELS_FORCED,PreviewType.STRING_LIST);
        settingDefaultValues.put(NSFW_IN_NSFW_CHANNELS_FORCED,"true");

        settingValues.put(NSFW_IN_NSFW_CHANNELS_FORCED, new String[]{"true","false"});
        settingsType.put(NSFW_IN_NSFW_CHANNELS_FORCED,NORMAL);
        settingDefaultValues.put(NSFW_IN_NSFW_CHANNELS_FORCED,"true");

        settingValues.put(SET_BOT_DISABLED, new String[]{"true","false"});
        settingsType.put(SET_BOT_DISABLED,NORMAL);
        settingDefaultValues.put(SET_BOT_DISABLED,"false");

        settingDefaultValues.put(USER_BLOCKLIST_LIST,new JSONObject().toString());
        settingDefaultValues.put(CHANNELS_DISABLED_LIST,new JSONObject().toString());

        previewType.put(USER_BLOCKLIST_ADD,PreviewType.USER_LIST);
        previewType.put(USER_BLOCKLIST_REMOVE,PreviewType.USER_LIST);

        previewType.put(CHANNELS_DISABLED_ADD,PreviewType.USER_LIST);
        previewType.put(CHANNELS_DISABLED_REMOVE,PreviewType.USER_LIST);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if(event.getMember() == null) {
            event.reply("An error occured. No Member detected.").setEphemeral(true).queue();
            return;
        }

        if(event.getGuild() == null) {
            event.reply("An error occured. No Guild detected.").setEphemeral(true).queue();
            return;
        }

        if(!(event.getMember().hasPermission(Permission.ADMINISTRATOR) || event.getMember().getId().equals("950863721243217980"))){
            event.reply("Sorry, but you dont have the permissions to edit settings.").setEphemeral(true).queue();
            return;
        }

        Setting setting = Setting.valueOf(event.getOption("key").getAsString());

        if (event.getName().equals("settings")) {
            String guildId = event.getGuild().getId();
            boolean hasValue = event.getOption("value") != null;
            String value = null;
            if(hasValue) value = event.getOption("value").getAsString();

            switch (setting) {
                case USER_BLOCKLIST_ADD -> {
                    JSONObject blocklist = new JSONObject(getSettingValue(USER_BLOCKLIST_LIST,guildId));
                    if(hasValue){
                        Member member = event.getGuild().getMemberById(value);
                        if(blocklist.has(value)){
                            event.reply("`" + member.getEffectiveName() + "` is already on the blocklist.").setEphemeral(true).queue();
                            return;
                        }
                        JSONObject block = new JSONObject();
                        block.put("date", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()));
                        block.put("blocked",true);
                        blocklist.put(value,block);
                        setSettingValue(USER_BLOCKLIST_LIST,guildId,blocklist.toString());
                        event.reply("Added `" + member.getEffectiveName() + "` to the user blocklist.").setEphemeral(true).queue();
                    } else {
                        event.reply("Please provide a value.").setEphemeral(true).queue();
                    }
                    return;
                }
                case USER_BLOCKLIST_REMOVE -> {
                    JSONObject blocklist = new JSONObject(getSettingValue(USER_BLOCKLIST_LIST,guildId));
                    if(hasValue){
                        Member member = event.getGuild().getMemberById(value);
                        if(blocklist.has(value)){
                            blocklist.remove(value);
                        } else {
                            event.reply("`" + member.getEffectiveName() + "` is not on the blocklist.").setEphemeral(true).queue();
                            return;
                        }
                        setSettingValue(USER_BLOCKLIST_LIST,guildId,blocklist.toString());
                        event.reply("Removed `" + member.getEffectiveName() + "` from the user blocklist.").setEphemeral(true).queue();
                    } else {
                        event.reply("Please provide a value.").setEphemeral(true).queue();
                    }
                    return;
                }
                case USER_BLOCKLIST_LIST -> {
                    JSONObject blocklist = new JSONObject(getSettingValue(USER_BLOCKLIST_LIST,guildId));
                    EmbedBuilder embedBuilder = new EmbedBuilder();
                    embedBuilder.setTitle("User Blocklist (" + blocklist.length() + ")");
                    embedBuilder.setDescription("Users blocked to use this bot.");
                    embedBuilder.setColor(0xff0000);
                    for(String id : blocklist.keySet()){
                        Member member = event.getGuild().getMemberById(id);
                        embedBuilder.addField(member.getEffectiveName(),"Blocked at " + blocklist.getJSONObject(id).getString("date"),true);
                    }
                    event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();
                    return;
                }
                case CHANNELS_DISABLED_ADD -> {
                    JSONObject disabledList = new JSONObject(getSettingValue(CHANNELS_DISABLED_LIST,guildId));
                    MessageChannelUnion channel = event.getChannel();
                    if(disabledList.has(channel.getId())){
                        event.reply("This channel is already disabled.").setEphemeral(true).queue();
                    } else {
                        disabledList.put(channel.getId(),true);
                        event.reply("This channel is now disabled.").setEphemeral(true).queue();
                    }
                    setSettingValue(CHANNELS_DISABLED_LIST,guildId,disabledList.toString());
                    return;
                }
                case CHANNELS_DISABLED_REMOVE -> {
                    JSONObject disabledList = new JSONObject(getSettingValue(CHANNELS_DISABLED_LIST,guildId));
                    MessageChannelUnion channel = event.getChannel();
                    if(disabledList.has(channel.getId())){
                        disabledList.remove(channel.getId());
                        setSettingValue(CHANNELS_DISABLED_LIST,guildId,disabledList.toString());
                        event.reply("This channel is now no longer disabled.").setEphemeral(true).queue();
                    } else {
                        event.reply("This channel is not disabled.").setEphemeral(true).queue();
                    }
                    return;
                }
                case CHANNELS_DISABLED_LIST -> {
                    JSONObject disabledList = new JSONObject(getSettingValue(CHANNELS_DISABLED_LIST,guildId));
                    EmbedBuilder embedBuilder = new EmbedBuilder();
                    embedBuilder.setTitle("Channels Disabled List (" + disabledList.length() + ")");
                    embedBuilder.setDescription("Channels where use o this bot is disabled.");
                    embedBuilder.setColor(0xff0000);
                    for(String id : disabledList.keySet()){
                        GuildChannel channel = event.getGuild().getGuildChannelById(id);
                        embedBuilder.addField(channel.getJumpUrl(),"Disabled",true);
                    }
                    event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();
                    return;
                }
            }

            if(event.getOptions().size() == 1) event.reply(event.getOption("key").getAsString() +": " + getSettingValue(Setting.valueOf(event.getOption("key").getAsString()),event.getGuild().getId())).setEphemeral(true).queue();
            if(event.getOptions().size() == 2) {
                setSettingValue(Setting.valueOf(event.getOption("key").getAsString()),event.getGuild().getId(),event.getOption("value").getAsString());
                event.reply(event.getOption("key").getAsString() + " is now set to " + getSettingValue(Setting.valueOf(event.getOption("key").getAsString()), event.getGuild().getId())).setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (event.getName().equals("settings")) {
            if(event.getFocusedOption().getName().equals("key")){
                List<String> suggestions = Arrays.stream(Setting.values())
                        .map(Enum::name)
                        .toList();

                List<String> filteredSuggestions = suggestions.stream()
                        .filter(s -> s.toLowerCase().contains(event.getFocusedOption().getValue().toLowerCase()))
                        .limit(25)
                        .toList();

                event.replyChoices(filteredSuggestions.stream()
                        .map(choice -> new net.dv8tion.jda.api.interactions.commands.Command.Choice(choice, choice))
                        .toList()
                ).queue();
            }
            if(event.getFocusedOption().getName().equals("value")){
                Setting setting = Setting.valueOf(event.getOption("key").getAsString());
                if(previewType.getOrDefault(setting, PreviewType.STRING_LIST).equals(PreviewType.USER_LIST)){
                    List<Command.Choice> suggestions = event.getGuild()
                            .getMembers().stream()
                            .filter(m -> m.getEffectiveName().toLowerCase().contains(event.getFocusedOption().getValue().toLowerCase()))
                            .limit(25)
                            .map(m -> new Command.Choice(m.getEffectiveName(), m.getId()))
                            .collect(Collectors.toList());

                    event.replyChoices(suggestions).queue();
                    return;
                }

                List<String> suggestions = new ArrayList<>(List.of(settingValues.get(Setting.valueOf(event.getOption("key").getAsString()))));

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

    public static void setSettingValue(Setting setting, String guildId, String newValue) {
        File file = new File("stonybot.settings");
        if(!file.exists()){
            try {
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(new JSONObject().toString(3).getBytes());
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        try {
            JSONObject data = new JSONObject(new String(new FileInputStream("stonybot.settings").readAllBytes()));
            JSONObject guildSettings = data.optJSONObject(guildId,new JSONObject());
            guildSettings.put(setting.name(),newValue);
            data.put(guildId,guildSettings);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data.toString(3).getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    public static String getSettingValue(Setting setting, String guildId) {
        File file = new File("stonybot.settings");
        if(!file.exists()){
            setSettingValue(setting, guildId, settingDefaultValues.get(setting));
        }
        try {
            JSONObject data = new JSONObject(new String(new FileInputStream("stonybot.settings").readAllBytes()));
            JSONObject guildObj = data.optJSONObject(guildId,new JSONObject());
            return guildObj.optString(setting.name(),settingDefaultValues.get(setting));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

enum SettingType{
    NORMAL, NO_ARG, THIS_CHANNEL_OPTION
}

enum PreviewType{
    STRING_LIST, USER_LIST
}