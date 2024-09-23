package me.zodd.postmanpat.econ

import github.scarsz.discordsrv.api.commands.PluginSlashCommand
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.Command
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.OptionData
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.SubcommandData
import me.zodd.postmanpat.PostmanPat.Companion.plugin
import me.zodd.postmanpat.Utils.EssxUtils.getEssxUser
import me.zodd.postmanpat.Utils.MessageUtils.replyEphemeralMessage
import me.zodd.postmanpat.command.PPSlashCommand
import me.zodd.postmanpat.command.PostmanCommandProvider
import me.zodd.postmanpat.econ.EconSlashCommands.EconCommands.Companion.pba
import java.awt.Color
import java.text.DecimalFormat


class EconSlashCommands : PostmanCommandProvider {

    enum class EconCommands(override val command: String) : PPSlashCommand<EconCommands> {
        ECON_PAY(plugin.configManager.conf.moduleConfig.econ.payCommand),
        ECON_BALANCE(plugin.configManager.conf.moduleConfig.econ.balCommand),
        ECON_FIRM_BASE(plugin.configManager.conf.moduleConfig.econ.firmBaseCommand),
        ECON_FIRM_PAY(plugin.configManager.conf.moduleConfig.econ.firmPayCommand)
        ;

        companion object {
            internal val pba: PlayerBusinessAddon? by lazy {
                if (plugin.server.pluginManager.getPlugin("DemocracyBusiness")?.isEnabled == true) {
                    PlayerBusinessAddon()
                } else null
            }
        }

        override fun exec(): (SlashCommandEvent) -> Unit {
            return when (this) {
                ECON_PAY -> this::payUserCommand
                ECON_BALANCE -> this::balanceUserCommand
                ECON_FIRM_BASE -> { _ -> /*This command is never run*/ }
                ECON_FIRM_PAY -> { s ->
                    pba?.firmPay(s) ?: run {
                        s.replyEphemeralMessage("Error Not loaded").queue()
                    }
                }
            }
        }

        private val config = plugin.configManager.conf
        private val econConfig = config.moduleConfig.econ
        private val decimalFormat = DecimalFormat(econConfig.decimalFormat)

        private val econ by lazy {
            plugin.econ ?: run {
                plugin.logger.warning("Econ not loaded, aborting!")
                null
            }
        }

        private fun payUserCommand(event: SlashCommandEvent) {
            val senderUser = getEssxUser(event) ?: run {
                event.replyEphemeralMessage("Unable to find User, account may not be linked!").queue()
                return
            }
            val offlineSender = plugin.server.getOfflinePlayer(senderUser.uuid)
            val econ = plugin.econ ?: return
            val senderBal = econ.getBalance(offlineSender)

            val target = event.getOption("user")?.asUser ?: return
            val targetUser = getEssxUser(target.id) ?: run {
                event.replyEphemeralMessage("Unable to find User, account may not be linked!").queue()
                return
            }

            if (!targetUser.isAcceptingPay) {
                event.replyEphemeralMessage("Target is not accepting pay at this time!").queue()
            }

            val amount = event.getOption("amount")?.asDouble ?: return

            if (amount <= econConfig.minimumSendable) {
                event.replyEphemeralMessage("Amount must be more than ${econConfig.minimumSendable}!").queue()
                return
            }

            if (senderBal < amount) {
                event.replyEphemeralMessage("You are too poor for this transaction!").queue()
                return
            }

            val offlineTarget = plugin.server.getOfflinePlayer(targetUser.uuid)

            // Attempt to withdraw money from sender
            val withdrawResult = econ.withdrawPlayer(offlineSender, amount)
            if (!withdrawResult.transactionSuccess()) {
                event.replyEphemeralMessage("Failed to withdraw money from ${senderUser.name}, transaction cancelled.")
                    .queue()
                return
            }
            // Attempt to deposit amount, or revert if failed.
            val depositResult = econ.depositPlayer(offlineTarget, amount)
            if (!depositResult.transactionSuccess()) {
                val revertResult = econ.depositPlayer(offlineSender, amount)
                if (!revertResult.transactionSuccess()) {
                    plugin.logger.warning("Failed to revert transaction. ${senderUser.name} may be owed $amount")
                    event.replyEphemeralMessage("Failed to revert transaction. Please contact an administrator.")
                        .queue()
                    return
                }
                event.replyEphemeralMessage("Failed to deposit money to ${targetUser.name}, transaction reverted.")
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

        private fun balanceUserCommand(event: SlashCommandEvent) {
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

    override fun slashCommands(): List<PluginSlashCommand> {
        val commands = mutableListOf(
            PluginSlashCommand(
                plugin, CommandData(EconCommands.ECON_PAY.command, "Pay's the target user a specified amount")
                    .addOption(OptionType.USER, "user", "user to send money to", true)
                    .addOption(OptionType.NUMBER, "amount", "amount to pay user", true)
            ),
            PluginSlashCommand(
                plugin, CommandData(EconCommands.ECON_BALANCE.command, "Checks the balance of the target or sender")
                    .addOption(OptionType.USER, "user", "User to check balance of", false)
            )
        )
        // Add command if PlayerBusinesses is enabled
        pba?.let { playerBusinessAddon ->
            commands.add(
                PluginSlashCommand(
                    plugin, CommandData(EconCommands.ECON_FIRM_BASE.command, "Base command for business transactions")
                        .addSubcommands(
                            SubcommandData(EconCommands.ECON_FIRM_PAY.command, "Command to pay from your business")
                                .addOptions(
                                    OptionData(
                                        OptionType.STRING,
                                        "business",
                                        "business to withdraw from",
                                        true
                                    ).addChoices(playerBusinessAddon.businesses.map {
                                        Command.Choice(
                                            it.name,
                                            it.name
                                        )
                                    })
                                )
                                .addOption(OptionType.USER, "user", "user to pay", true)
                                .addOption(OptionType.NUMBER, "amount", "amount to pay another user", true)
                        )
                )
            )
        }

        return commands
    }
}

