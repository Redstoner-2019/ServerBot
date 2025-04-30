package me.redstoner2019.utils;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class LongMessageSender {
    private static final int DISCORD_MESSAGE_LIMIT = 2000;

    public static void sendLongMessage(TextChannel channel, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }

        int start = 0;
        while (start < message.length()) {
            int end = Math.min(start + DISCORD_MESSAGE_LIMIT, message.length());

            // Try to split at a newline or space if possible to avoid mid-word breaks
            if (end < message.length()) {
                int lastNewline = message.lastIndexOf("\n", end);
                int lastSpace = message.lastIndexOf(" ", end);
                int split = Math.max(lastNewline, lastSpace);

                if (split > start) {
                    end = split;
                }
            }

            String chunk = message.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                channel.sendMessage(chunk).queue();
            }

            start = end;
        }
    }
}
