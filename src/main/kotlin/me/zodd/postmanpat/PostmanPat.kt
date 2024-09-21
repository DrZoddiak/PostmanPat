package me.zodd.postmanpat

import com.earth2me.essentials.IEssentials
import github.scarsz.discordsrv.DiscordSRV
import github.scarsz.discordsrv.api.commands.PluginSlashCommand
import github.scarsz.discordsrv.api.commands.SlashCommand
import github.scarsz.discordsrv.api.commands.SlashCommandProvider
import github.scarsz.discordsrv.dependencies.jda.api.JDA
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent
import github.scarsz.discordsrv.util.DiscordUtil
import me.zodd.postmanpat.config.ConfigManager
import me.zodd.postmanpat.config.PostmanPatConfig
import me.zodd.postmanpat.econ.EconSlashCommands
import me.zodd.postmanpat.econ.EconSlashCommands.EconCommands.*
import me.zodd.postmanpat.mail.MailListeners
import me.zodd.postmanpat.mail.MailSlashCommands
import me.zodd.postmanpat.mail.MailSlashCommands.MailCommands.*
import me.zodd.postmanpat.mail.MailUserStorage
import net.essentialsx.api.v2.events.UserMailEvent
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin


class PostmanPat : JavaPlugin(), SlashCommandProvider {
    val srv: DiscordSRV = DiscordSRV.getPlugin()
    val ess: IEssentials? = server.pluginManager.getPlugin("Essentials") as IEssentials?

    val configManager by lazy { ConfigManager(plugin, "postmanpatConfig", PostmanPatConfig::class) }
    val userStorageManager by lazy { ConfigManager(plugin, "userStorage", MailUserStorage::class) }

    val econ: Economy? by lazy {
        loadEcon() ?: run {
            server.pluginManager.disablePlugin(this)
            logger.warning("Failed to load economy! This plugin requires Vault!")
            null
        }
    }

    val jda: JDA
        get() = DiscordUtil.getJda()

    companion object {
        val plugin by lazy { getPlugin(PostmanPat::class.java) }
    }

    override fun onEnable() {
        // Initializes ConfigManagers
        userStorageManager
        configManager

        Bukkit.getPluginManager().registerEvent(
            UserMailEvent::class.java, object : Listener {
            }, EventPriority.NORMAL,
            { exec: Listener?, e: Event ->
                MailListeners(this).userMailListener(exec, e)
            }, this
        )
    }

    private fun loadEcon(): Economy? {
        server.pluginManager.getPlugin("Vault") ?: return null
        return server.servicesManager.getRegistration(Economy::class.java)?.provider ?: return null
    }

    @SlashCommand(path = "*")
    fun processMailSlashCommand(event: SlashCommandEvent) {
        when (event.commandPath.substringBefore("/")) {
            ECON_PAY.command -> ECON_PAY
            ECON_BALANCE.command -> ECON_BALANCE
            MAIL_BASE.command -> when (event.subcommandName) {
                MAIL_READ.command -> MAIL_READ
                MAIL_SEND.command -> MAIL_SEND
                MAIL_IGNORE.command -> MAIL_IGNORE
                MAIL_MARK_READ.command -> MAIL_MARK_READ
                else -> null
            }

            ECON_FIRM_BASE.command -> when (event.subcommandName) {
                ECON_FIRM_PAY.command -> ECON_FIRM_PAY
                else -> null
            }

            else -> null
        }?.exec()?.invoke(event)
    }

    override fun onDisable() {
        userStorageManager.save()
    }

    override fun getSlashCommands(): MutableSet<PluginSlashCommand> {
        return HashSet(
            mutableListOf<PluginSlashCommand>().apply {
                addAll(EconSlashCommands().slashCommands())
                addAll(MailSlashCommands().slashCommands())
            }
        )
    }
}