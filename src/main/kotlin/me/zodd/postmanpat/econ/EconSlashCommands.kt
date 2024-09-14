package me.zodd.postmanpat.econ;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent
import me.zodd.postmanpat.EssxData
import me.zodd.postmanpat.PostmanPat
import me.zodd.postmanpat.config.PostmanPatConfig
import java.awt.Color
import java.text.DecimalFormat

class EconSlashCommands(private val plugin: PostmanPat) : EssxData(plugin) {
    private val config = PostmanPatConfig.config
    private val econConfig = config.commandConfig.econCommands
    private val decimalFormat = DecimalFormat(econConfig.decimalFormat)

    private val econ by lazy {
        plugin.econ ?: run {
            plugin.logger.info("Econ not loaded, aborting!")
            null
        }
    }

    fun payUserCommand(event: SlashCommandEvent) {
        val senderUser = getEssxUser(event) ?: run {
            event.reply("Unable to find User, account may not be linked!").setEphemeral(true).queue()
            return
        }
        val offlineSender = plugin.server.getOfflinePlayer(senderUser.uuid)
        val econ = plugin.econ ?: return
        val senderBal = econ.getBalance(offlineSender)

        val target = event.getOption("user")?.asUser ?: return
        val targetUser = getEssxUser(target.id) ?: run {
            event.reply("Unable to find User, account may not be linked!").setEphemeral(true).queue()
            return
        }

        if (!targetUser.isAcceptingPay) {
            event.reply("Target is not accepting pay at this time!").setEphemeral(true).queue()
        }

        val amount = event.getOption("amount")?.asDouble ?: return

        if (amount <= econConfig.minimumSendable) {
            event.reply("Amount must be more than ${econConfig.minimumSendable}!").setEphemeral(true).queue()
            return
        }

        if (senderBal < amount) {
            event.reply("You are too poor for this transaction!").setEphemeral(true).queue()
            return
        }

        val offlineTarget = plugin.server.getOfflinePlayer(targetUser.uuid)

        // Attempt to withdraw money from sender
        val withdrawResult = econ.withdrawPlayer(offlineSender, amount)
        if (!withdrawResult.transactionSuccess()) {
            event.reply("Failed to withdraw money from ${senderUser.name}, transaction cancelled.")
                .setEphemeral(true).queue()
            return
        }

        // Attempt to deposit amount, or revert if failed.
        val depositResult = econ.depositPlayer(offlineTarget, amount)
        if (!depositResult.transactionSuccess()) {
            val revertResult = econ.depositPlayer(offlineSender, amount)
            if (!revertResult.transactionSuccess()) {
                plugin.logger.warning("Failed to revert transaction. ${senderUser.name} may be owed $amount")
                event.reply("Failed to revert transaction. Please contact an administrator.")
                return
            }
            event.reply("Failed to deposit money to ${targetUser.name}, transaction reverted.")
                .setEphemeral(true)
                .queue()
            return
        }

        val embed = EmbedBuilder().apply {
            setTitle("Payment")
            setColor(Color.green)
            setDescription("You have sent ${econConfig.currencySymbol}${decimalFormat.format(amount)} to ${targetUser.name}")
            setFooter(config.serverBranding)
        }.build()

        event.replyEmbeds(embed).queue()
    }

    fun balanceUserCommand(event: SlashCommandEvent) {
        plugin.logger.info("Balance Command Fired")
        val senderUser = getEssxUser(event) ?: return
        val target = event.getOption("user")
        val targetUser = target?.let { getEssxUser(it.asUser.id) } ?: senderUser
        val balance = econ?.getBalance(plugin.server.getOfflinePlayer(targetUser.uuid)) ?: return

        val embed = EmbedBuilder().apply {
            setTitle("Balance for ${targetUser.name}")
            setColor(Color.green)
            setDescription("They currently have ${econConfig.currencySymbol}${decimalFormat.format(balance)} available in their in-game balance")
            setFooter(config.serverBranding)
        }.build()

        event.replyEmbeds(embed).queue()
    }
}