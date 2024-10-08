package me.zodd.postmanpat.mail;

import me.zodd.postmanpat.EssxData;
import me.zodd.postmanpat.PostmanPat;
import me.zodd.postmanpat.PostmanPatConfig;
import net.essentialsx.api.v2.events.UserMailEvent;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;

import java.util.ArrayList;

public class MailListeners extends EssxData {
    PostmanPat plugin;

    public MailListeners(PostmanPat plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    public void userMailListener(Listener exec, Event e) {
        var event = (UserMailEvent) e;
        if (plugin.getJda() == null) {
            plugin.getLogger().warning("JDA not available");
            return;
        }

        if (mgr() == null) {
            plugin.getLogger().warning("Account link manager unavailable");
            return;
        }

        var msg = event.getMessage();
        var senderUUID = msg.getSenderUUID();
        var senderUser = getEssxUser(senderUUID);

        var mailManager = new DiscordMailManager(msg);
        var recipient = event.getRecipient();
        var discordID = mgr().getDiscordId(recipient.getUUID());
        if (discordID == null) {
            plugin.getLogger().warning("Failed to retrieve discord ID, account may not be linked.");
            return;
        }
        var user = plugin.getJda().getUserById(discordID);
        if (user == null) {
            plugin.getLogger().warning("Failed to get user by ID, are they in the guild?");
            return;
        }

        var ignores = MailUserStorage.mailIgnoreList.getOrDefault(recipient.getUUID(), new ArrayList<>());

        if (getEssxUser(user.getId()).isIgnoredPlayer(senderUser) || (!ignores.isEmpty() && ignores.contains(senderUUID))) {
            // Don't send a message if ignoring player
            // They will still receive the mail, just not through discord
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
