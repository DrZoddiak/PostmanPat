package me.zodd.postmanpat

import com.earth2me.essentials.IEssentials
import github.scarsz.discordsrv.DiscordSRV
import github.scarsz.discordsrv.api.commands.PluginSlashCommand
import github.scarsz.discordsrv.api.commands.SlashCommand
import github.scarsz.discordsrv.api.commands.SlashCommandProvider
import github.scarsz.discordsrv.dependencies.jda.api.JDA
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.SubcommandData
import github.scarsz.discordsrv.util.DiscordUtil
import me.zodd.postmanpat.config.ConfigManager
import me.zodd.postmanpat.config.PostmanPatConfig
import me.zodd.postmanpat.econ.EconSlashCommands
import me.zodd.postmanpat.mail.MailListeners
import me.zodd.postmanpat.mail.MailSlashCommands
import me.zodd.postmanpat.mail.MailUserStorage
import net.essentialsx.api.v2.events.UserMailEvent
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

class PostmanPat : JavaPlugin(), SlashCommandProvider {
    val srv: DiscordSRV = DiscordSRV.getPlugin()
    val ess: IEssentials? = server.pluginManager.getPlugin("Essentials") as IEssentials?

    val econ: Economy? by lazy {
        loadEcon() ?: run {
            server.pluginManager.disablePlugin(this)
            logger.info("Failed to load economy! This plugin requires Vault!")
            null
        }
    }

    companion object {
        val plugin by lazy { getPlugin(PostmanPat::class.java) }
        val configManager by lazy { ConfigManager(plugin, "postmanpatConfig", PostmanPatConfig::class) }
        val userStorageManager by lazy { ConfigManager(plugin, "userStorage", MailUserStorage::class) }

    }

    override fun onEnable() {
        Bukkit.getPluginManager().registerEvent(
            UserMailEvent::class.java, object : Listener {
            }, EventPriority.NORMAL,
            { exec: Listener?, e: Event ->
                MailListeners(this).userMailListener(exec, e)
            }, this
        )
    }

    private fun loadEcon(): Economy? {
        // Verify Vault is loaded
        server.pluginManager.getPlugin("Vault") ?: return null
        val rsp = server.servicesManager.getRegistration(Economy::class.java) ?: return null
        return rsp.provider
    }


    enum class MailCommands(override val command: String) : PPSlashCommand<MailCommands> {
        MAIL_BASE(configManager.conf.commandConfig.mailCommands.baseCommand),
        MAIL_READ(configManager.conf.commandConfig.mailCommands.readSubCommand),
        MAIL_SEND(configManager.conf.commandConfig.mailCommands.sendSubCommand),
        MAIL_MARK_READ(configManager.conf.commandConfig.mailCommands.markReadSubCommand),
        MAIL_IGNORE(configManager.conf.commandConfig.mailCommands.ignoreSubCommand);


        private val mailCommands: MailSlashCommands = MailSlashCommands(plugin)

        override fun exec(): (SlashCommandEvent) -> Unit {
            return when (this) {
                MAIL_READ -> mailCommands::mailReadCommand
                MAIL_SEND -> mailCommands::mailSendCommand
                MAIL_MARK_READ -> mailCommands::markAsReadCommand
                MAIL_IGNORE -> mailCommands::ignoreUserCommand
                MAIL_BASE -> { _ -> /*This command is never run*/ }
            }
        }
    }

    enum class EconCommands(override val command: String) : PPSlashCommand<EconCommands> {
        ECON_PAY(configManager.conf.commandConfig.econCommands.payCommand),
        ECON_BALANCE(configManager.conf.commandConfig.econCommands.balCommand),
        ;

        private val mailCommands: EconSlashCommands = EconSlashCommands(plugin)

        override fun exec(): (SlashCommandEvent) -> Unit {
            return when (this) {
                ECON_PAY -> mailCommands::payUserCommand
                ECON_BALANCE -> mailCommands::balanceUserCommand
            }
        }
    }

    interface PPSlashCommand<T : PPSlashCommand<T>> {
        val command: String

        fun exec(): (SlashCommandEvent) -> Unit
    }


    // todo: Convert to a single path (*) and write a command dispatcher
    @SlashCommand(path = "*")
    fun processMailSlashCommand(event: SlashCommandEvent) {

        when (event.commandPath) {
            EconCommands.ECON_PAY.command -> EconCommands.ECON_PAY
            EconCommands.ECON_BALANCE.command -> EconCommands.ECON_BALANCE
            MailCommands.MAIL_BASE.command -> when (event.subcommandName) {
                MailCommands.MAIL_READ.command -> MailCommands.MAIL_READ
                MailCommands.MAIL_SEND.command -> MailCommands.MAIL_SEND
                MailCommands.MAIL_IGNORE.command -> MailCommands.MAIL_IGNORE
                MailCommands.MAIL_MARK_READ.command -> MailCommands.MAIL_MARK_READ
                else -> null
            }

            else -> null
        }?.exec()?.invoke(event) ?: run {
            logger.info("Could not find command")
        }
    }


    override fun onDisable() {
        //saveMailUserStorage()
    }

    val jda: JDA
        get() = DiscordUtil.getJda()

    override fun getSlashCommands(): Set<PluginSlashCommand> {
        return HashSet(
            listOf(
                PluginSlashCommand(
                    this, CommandData("mail", "mail command")
                        .addSubcommands(
                            SubcommandData(MailCommands.MAIL_READ.command, "read mail")
                                .addOption(OptionType.INTEGER, "page", "page number to view", false)
                                .addOption(
                                    OptionType.BOOLEAN,
                                    configManager.conf.commandConfig.mailCommands.includeReadArg,
                                    "include all mail including already read"
                                ),

                            SubcommandData(MailCommands.MAIL_SEND.command, "send mail")
                                .addOption(OptionType.USER, "user", "user to send mail to.", true)
                                .addOption(OptionType.STRING, "message", "message to send to the user", true),

                            SubcommandData(MailCommands.MAIL_MARK_READ.command, "Marks all mail as having been read."),

                            SubcommandData(MailCommands.MAIL_IGNORE.command, "Toggle receiving messages from a user")
                                .addOption(OptionType.USER, "user", "User to toggle ignoring", false)
                                .addOption(OptionType.STRING, "uuid", "Use a players UUID directly", false)
                        )
                ),
                PluginSlashCommand(
                    this, CommandData(EconCommands.ECON_PAY.command, "Pay's the target user a specified amount")
                        .addOption(OptionType.USER, "user", "user to send money to", true)
                        .addOption(OptionType.NUMBER, "amount", "amount to pay user", true)
                ),
                PluginSlashCommand(
                    this, CommandData(EconCommands.ECON_BALANCE.command, "Checks the balance of the target or sender")
                        .addOption(OptionType.USER, "user", "User to check balance of", false)
                )

            )
        )
    }
}