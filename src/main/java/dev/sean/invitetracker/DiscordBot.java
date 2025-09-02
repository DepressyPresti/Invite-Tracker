package dev.sean.invitetracker;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.awt.*;
import java.time.Instant;
import java.util.EnumSet;

public class DiscordBot {

    private final JavaPlugin plugin;
    private final String token;
    private net.dv8tion.jda.api.JDA jda;

    public DiscordBot(JavaPlugin plugin, String token) {
        this.plugin = plugin;
        this.token = token;
    }

    public void start() {
        try {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(EnumSet.of(
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT
                    ))
                    .setAutoReconnect(true)
                    .build();
            jda.awaitReady();
            plugin.getLogger().info("Discord bot logged in as " + jda.getSelfUser().getAsTag());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to start Discord bot: " + e.getMessage());
        }
    }

    public boolean isReady() {
        return jda != null && jda.getStatus() == net.dv8tion.jda.api.JDA.Status.CONNECTED;
    }

    public void shutdown() {
        if (jda != null) {
            jda.shutdownNow();
        }
    }

    public void deleteMessage(String channelId, String messageId) {
        if (!isReady()) return;
        MessageChannel ch = jda.getTextChannelById(channelId);
        if (ch == null) return;
        try {
            ch.deleteMessageById(messageId).queue(
                    success -> {},
                    failure -> {}
            );
        } catch (Exception ignored) {}
    }

    @Nullable
    public String sendJoinEmbed(String channelId,
                                String contentPing,
                                String title,
                                String mcUsername,
                                String domain,
                                int totalInvites,
                                int currentMilestoneIndex,
                                int milestoneJustReached) {
        if (!isReady()) return null;

        MessageChannel ch = jda.getTextChannelById(channelId);
        if (ch == null) {
            plugin.getLogger().warning("Discord channel " + channelId + " not found.");
            return null;
        }

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(title);
        eb.addField("Player", mcUsername, true);
        eb.addField("Domain", domain, true);
        eb.addField("Total Invites", String.valueOf(totalInvites), true);
        eb.setFooter("Invites: " + totalInvites + " • Milestone: " + currentMilestoneIndex);
        eb.setTimestamp(Instant.now());
        eb.setColor(milestoneJustReached > 0 ? Color.GREEN : Color.BLUE);

        MessageCreateBuilder msg = new MessageCreateBuilder()
                .setContent(contentPing)
                .setEmbeds(eb.build());

        try {
            return ch.sendMessage(msg.build()).complete().getId();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send Discord message: " + e.getMessage());
            return null;
        }
    }
}
