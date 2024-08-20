package me.zodd.postmanpat.econ;

import com.earth2me.essentials.User;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import me.zodd.postmanpat.EssxData;
import me.zodd.postmanpat.PostmanPat;
import me.zodd.postmanpat.PostmanPatConfig;

import java.awt.Color;

public class EconSlashCommands extends EssxData {
    PostmanPat plugin;

    public EconSlashCommands(PostmanPat plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    public void payUserCommand(SlashCommandEvent event) {
        var senderUser = getEssxUser(event);
        var offlineSender = plugin.getServer().getOfflinePlayer(senderUser.getUUID());
        var senderBal = plugin.getEcon().getBalance(offlineSender);
        var target = event.getOption("user").getAsUser();
        var targetUser = getEssxUser(target.getId());

        if (!targetUser.isAcceptingPay()) {
            // Handle target not accepting pay
            event.reply("Target is not accepting pay right now!").setEphemeral(true).queue();
            return;
        }

        var amount = event.getOption("amount").getAsDouble();

        if (amount <= 0) {
            event.reply("Amount must be more than 0!").setEphemeral(true).queue();
            return;
        }

        if (senderBal < amount) {
            event.reply("You are too poor for this transaction!").setEphemeral(true).queue();
            return;
        }

        var offlineTarget = plugin.getServer().getOfflinePlayer(targetUser.getUUID());

        var withdrawRes = plugin.getEcon().withdrawPlayer(offlineSender, amount);
        if (!withdrawRes.transactionSuccess()) {
            event.reply("Failed to withdraw money from player, transaction cancelled.").setEphemeral(true).queue();
            return;
        }

        var depositRes = plugin.getEcon().depositPlayer(offlineTarget, amount);
        if (!depositRes.transactionSuccess()) {
            var revertRes = plugin.getEcon().depositPlayer(offlineSender, amount);
            if (!revertRes.transactionSuccess()) {
                plugin.getLogger().warning("Failed to revert transaction. "
                        + senderUser.getName()
                        + " may be due " + amount);
            }
            event.reply("Failed to deposit money to player, transaction reverted.").setEphemeral(true).queue();
            return;
        }

        var embed = new EmbedBuilder()
                .setTitle("Payment")
                .setColor(Color.green)
                .setDescription("You have sent " + PostmanPatConfig.currencySymbol + amount + " to " + targetUser.getName())
                .setFooter(PostmanPatConfig.serverBranding)
                .build();

        event.replyEmbeds(embed).queue();
    }

    public void balanceUserCommand(SlashCommandEvent event) {
        var senderUser = getEssxUser(event);

        var target = event.getOption("user");
        User targetUser;
        if (target == null) {
            targetUser = senderUser;
        } else {
            targetUser = getEssxUser(target.getAsUser().getId());
        }

        var balance = plugin.getEcon().getBalance(plugin.getServer().getOfflinePlayer(targetUser.getUUID()));

        var embed = new EmbedBuilder()
                .setTitle("Balance for " + targetUser.getName())
                .setColor(Color.green)
                .setDescription("They currently have " + PostmanPatConfig.currencySymbol + balance + " available in their in-game balance")
                .setFooter(PostmanPatConfig.serverBranding)
                .build();

        event.replyEmbeds(embed).queue();

    }
}