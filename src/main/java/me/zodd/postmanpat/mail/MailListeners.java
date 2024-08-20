package me.zodd.postmanpat.mail;

import me.zodd.postmanpat.PostmanPat;
import me.zodd.postmanpat.PostmanPatConfig;
import net.essentialsx.api.v2.events.UserMailEvent;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;

public class MailListeners {
    PostmanPat plugin;

    public MailListeners(PostmanPat plugin) {
        this.plugin = plugin;
    }

    public void userMailListener(Listener exec, Event e) {
        var event = (UserMailEvent) e;
        if (plugin.getJda() == null) {
            plugin.getLogger().warning("JDA not available");
            return;
        }

        if (plugin.mgr() == null) {
            plugin.getLogger().warning("Account link manager unavailable");
            return;
        }

        var msg = event.getMessage();
        var mailManager = new DiscordMailManager(msg);
        var p = event.getRecipient();
        var discordID = plugin.mgr().getDiscordId(p.getUUID());
        if (discordID == null) {
            plugin.getLogger().warning("Failed to retrieve discord ID, account may not be linked.");
            return;
        }
        var user = plugin.getJda().getUserById(discordID);
        if (user == null) {
            plugin.getLogger().warning("Failed to get user by ID, are they in the guild?");
            return;
        }

        var content = mailManager.formatMailMessage(new StringBuilder("[New Mail!]"), msg).toString();

        user.openPrivateChannel().queue(c -> {
            mailManager.splitContent(content).forEach(m -> c.sendMessage(m).queue(s -> {
            }, err -> {
                // If we're unable to send a DM to the user
                var channelID = PostmanPatConfig.NotifyChannel;
                var channel = plugin.getJda().getTextChannelById(channelID);
                if (channel == null) {
                    plugin.getLogger().info("Unable to find configured discord channel! " + channelID);
                    return;
                }
                channel.sendMessage(user.getAsMention() + " You have received mail! Check it with `/mail read` !").queue();
            }));
        });
    }
}
