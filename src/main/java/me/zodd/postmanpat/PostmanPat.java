package me.zodd.postmanpat;

import com.earth2me.essentials.IEssentials;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.commands.PluginSlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommandProvider;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.SubcommandData;
import github.scarsz.discordsrv.objects.managers.AccountLinkManager;
import github.scarsz.discordsrv.util.DiscordUtil;
import me.zodd.postmanpat.mail.DiscordMailManager;
import me.zodd.postmanpat.mail.MailListeners;
import me.zodd.postmanpat.mail.MailSlashCommands;
import net.essentialsx.api.v2.events.UserMailEvent;
import net.essentialsx.api.v2.services.mail.MailMessage;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import redempt.redlib.config.ConfigManager;

import java.time.Instant;
import java.util.*;

public final class PostmanPat extends JavaPlugin implements SlashCommandProvider {
    DiscordSRV srv = DiscordSRV.getPlugin();
    IEssentials ess = (IEssentials) getServer().getPluginManager().getPlugin("Essentials");
    MailSlashCommands mailCommands = new MailSlashCommands(ess, srv);

    @Override
    public void onEnable() {
        loadConfig();

        Bukkit.getPluginManager().registerEvent(UserMailEvent.class, new Listener() {
        }, EventPriority.NORMAL, new MailListeners(this)::userMailListener, this);
    }

    void loadConfig() {
        ConfigManager.create(this).target(PostmanPatConfig.class).saveDefaults().load();
    }

    @Override
    public void onDisable() {}

    @Override
    public Set<PluginSlashCommand> getSlashCommands() {
        return new HashSet<>(List.of(
                new PluginSlashCommand(this, new CommandData(PostmanPatConfig.rootCommand, "mail command")
                        .addSubcommands(
                                new SubcommandData(PostmanPatConfig.readSubCommand, "read mail")
                                        .addOption(OptionType.INTEGER, "page", "page number to view", false)
                                        .addOption(OptionType.BOOLEAN, PostmanPatConfig.includeReadArg, "include all mail including already read"),
                                new SubcommandData(PostmanPatConfig.sendSubCommand, "send mail")
                                        .addOption(OptionType.USER, "user", "user to send mail to.", true)
                                        .addOption(OptionType.STRING, "message", "message to send to the user", true),
                                new SubcommandData(PostmanPatConfig.markReadSubCommand, "Marks all mail as having been read.")
                        )
                )
        ));
    }



    @SlashCommand(path = "mail/*")
    public void processMailSlashCommand(SlashCommandEvent event) {
        var sub = event.getSubcommandName();
        if (sub.contentEquals(PostmanPatConfig.readSubCommand)) {
            mailCommands.mailReadCommand(event);
        } else if (sub.contentEquals(PostmanPatConfig.sendSubCommand)) {
            mailCommands.mailSendCommand(event);
        } else if (sub.contentEquals(PostmanPatConfig.markReadSubCommand)) {
            mailCommands.markAsReadCommand(event);
        }
    }

    public AccountLinkManager mgr() {
        return srv.getAccountLinkManager();
    }

    public JDA getJda() {
        return DiscordUtil.getJda();
    }
}

