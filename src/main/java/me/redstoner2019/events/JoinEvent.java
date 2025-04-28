package me.redstoner2019.events;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class JoinEvent extends ListenerAdapter {
    @Override
    public void onGuildJoin(GuildJoinEvent event){
        Guild g = event.getGuild();
        g.updateCommands().addCommands(
                Commands.slash("vc","The vc command!")
                        .addOption(OptionType.INTEGER, "speed", "Speed of the tts!", false)
                        .addOption(OptionType.INTEGER, "pitch", "Pitch of the tts!", false)
                        .addOption(OptionType.STRING, "alias", "Alias when reading your message!", false)
                        .addOption(OptionType.STRING, "voice", "Set your voice!", false,true)
                        .addOption(OptionType.STRING, "say", "Say something in VC!", false),
                Commands.slash("zitat","Sucht ein random zitat!"),
                Commands.slash("sfx","Spiel einen SFX ab!")
                        .addOption(OptionType.STRING, "sound", "Der sound!", true,true),
                Commands.slash("voices","Liste alle Stimmen auf!")
        ).queue();
    }
}
