package me.zodd.postmanpat.econ

import github.scarsz.discordsrv.api.commands.PluginSlashCommand
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.Command
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.OptionData
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.SubcommandData
import me.zodd.postmanpat.PostmanPat
import me.zodd.postmanpat.PostmanPat.Companion.plugin
import me.zodd.postmanpat.Utils.EssxUtils.getEssxUser
import me.zodd.postmanpat.Utils.MessageUtils.embedMessage
import me.zodd.postmanpat.Utils.MessageUtils.replyEphemeral
import me.zodd.postmanpat.Utils.MessageUtils.replyEphemeralEmbed
import me.zodd.postmanpat.Utils.SlashCommandUtils.get
import me.zodd.postmanpat.command.PPSlashCommand
import me.zodd.postmanpat.command.PostmanCommandProvider
import me.zodd.postmanpat.econ.EconSlashCommands.EconCommands.Companion.pba
import me.zodd.postmanpat.econ.entity.UserEntity


class EconSlashCommands : PostmanCommandProvider {

    enum class EconCommands(override val command: String) : PPSlashCommand<EconCommands> {
        ECON_PAY(plugin.configManager.conf.moduleConfig.econ.payCommand),
        ECON_BALANCE(plugin.configManager.conf.moduleConfig.econ.balCommand),
        ECON_FIRM_BASE(plugin.configManager.conf.moduleConfig.econ.firmBaseCommand),
        ECON_FIRM_PAY(plugin.configManager.conf.moduleConfig.econ.firmPayCommand),
        ECON_FIRM_LIST(plugin.configManager.conf.moduleConfig.econ.firmListBusinesses),
        ECON_FIRM_BALANCE(plugin.configManager.conf.moduleConfig.econ.firmBalCommand),
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
                ECON_FIRM_PAY -> { s -> // Reply should never send in theory, as the command shouldn't be loaded
                    pba?.firmPay(s) ?: s.replyEphemeral("Error Not loaded").queue()
                }

                ECON_FIRM_LIST -> { s ->
                    pba?.listOwnedBusinesses(s) ?: s.replyEphemeral("Error Not loaded").queue()
                }

                ECON_FIRM_BALANCE -> { s ->
                    pba?.firmBal(s) ?: s.replyEphemeral("Error Not loaded").queue()
                }
            }
        }

        private val config = plugin.configManager.conf
        private val econConfig = config.moduleConfig.econ
        private val decimalFormat = econConfig.decimalFormat()

        private fun payUserCommand(event: SlashCommandEvent) {
            val senderUser = getEssxUser(event) ?: run {
                event.replyEphemeral("Unable to find User, account may not be linked!").queue()
                return
            }

            val targetUser = event["user"]?.asUser?.id?.let { getEssxUser(it) }
                ?: event["player"]?.asString?.let { plugin.server.getOfflinePlayer(it) }
                    ?.let { getEssxUser(it.uniqueId) } ?: run {
                    event.replyEphemeral("Unable to find user! Ensure name is spelled correctly or try @tagging them")
                        .queue()
                    return
                }

            /*            val target = event.getOption("user")?.asUser?.id ?: return
                        val targetUser = getEssxUser(target) ?: run {
                            event.replyEphemeral("Unable to find User, account may not be linked!").queue()
                            return
                        }*/

            val sender = UserEntity(senderUser)
            val receiver = UserEntity(targetUser)
            PostmanEconManager(sender, event).transferFunds(receiver)
        }

        private fun balanceUserCommand(event: SlashCommandEvent) {
            val senderUser = getEssxUser(event) ?: return
            val user = event["user"]
            val targetUser = user?.let { getEssxUser(it.asUser.id) } ?: senderUser

            val target = UserEntity(targetUser)

            event.replyEphemeralEmbed(
                embedMessage(
                    "Balance for ${target.name}",
                    "They currently have ${econConfig.currencySymbol}${decimalFormat.format(target.balance)} available in business balance"
                )
            ).queue()
        }
    }

    override fun slashCommands(): List<PluginSlashCommand> {
        val commands = mutableListOf(
            PluginSlashCommand(
                plugin, CommandData(EconCommands.ECON_PAY.command, "Pay's the target user a specified amount")
                    .addOption(OptionType.NUMBER, "amount", "amount to pay user", true)
                    .addOption(OptionType.USER, "user", "user to pay by @tag", false)
                    .addOption(OptionType.STRING, "player", "player to pay by username", false)
            ),
            PluginSlashCommand(
                plugin, CommandData(EconCommands.ECON_BALANCE.command, "Checks the balance of the target or sender")
                    .addOption(OptionType.USER, "user", "user to pay", false)
                    .addOption(OptionType.STRING, "player", "player to pay", false)
            )
        )
        // Add command if PlayerBusinesses is enabled
        pba?.let {
            commands.add(
                PluginSlashCommand(
                    plugin, CommandData(EconCommands.ECON_FIRM_BASE.command, "Base command for business transactions")
                        .addSubcommands(
                            SubcommandData(EconCommands.ECON_FIRM_PAY.command, "Command to pay from your business")
                                .addOption(OptionType.STRING, "business", "business to pay from", true)
                                .addOption(OptionType.NUMBER, "amount", "amount to pay another user", true)
                                .addOption(OptionType.USER, "user", "user to pay by @tag", false)
                                .addOption(OptionType.STRING, "player", "player to pay by username", false),
                            SubcommandData(
                                EconCommands.ECON_FIRM_BALANCE.command,
                                "Command to check your businesses balance"
                            ).addOption(OptionType.STRING, "business", "business to check balance of", true),
                            SubcommandData(
                                EconCommands.ECON_FIRM_LIST.command,
                                "Lists businesses you have financial access to"
                            )
                        )

                )
            )
        }

        return commands
    }
}

