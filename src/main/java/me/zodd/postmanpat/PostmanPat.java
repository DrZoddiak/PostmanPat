package me.zodd.postmanpat;

import com.earth2me.essentials.IEssentials;
import com.earth2me.essentials.User;
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
import net.essentialsx.api.v2.events.UserMailEvent;
import net.essentialsx.api.v2.services.mail.MailMessage;
import org.bukkit.Bukkit;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.java.JavaPlugin;
import redempt.redlib.config.ConfigManager;
import redempt.redlib.config.annotations.Comment;
import redempt.redlib.config.annotations.ConfigMappable;
import redempt.redlib.config.annotations.ConfigPath;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class PostmanPat extends JavaPlugin implements SlashCommandProvider {
    // Discords message content limit is 2000
    // We go 100 under to give ourselves room for metadata
    final int MESSAGE_CONTENT_LIMIT = 1900;

    DiscordSRV srv = DiscordSRV.getPlugin();
    IEssentials ess = (IEssentials) getServer().getPluginManager().getPlugin("Essentials");

    @Override
    public void onEnable() {
        ConfigManager.create(this).target(PostmanPatConfig.class).saveDefaults().load();

        EventExecutor eventExecutor = (exec, e) -> {
            var event = (UserMailEvent) e;
            JDA jda = getJda();
            if (jda == null) {
                getLogger().warning("JDA not available");
                return;
            }

            if (mgr() == null) {
                getLogger().warning("Account link manager unavailable");
                return;
            }

            var msg = event.getMessage();
            var mailManager = new DiscordMailManager(msg);
            var p = event.getRecipient();
            var discordID = mgr().getDiscordId(p.getUUID());
            if (discordID == null) {
                getLogger().warning("Failed to retrieve discord ID, account may not be linked.");
                return;
            }
            var user = jda.getUserById(discordID);
            if (user == null) {
                getLogger().warning("Failed to get user by ID, are they in the guild?");
                return;
            }

            var content = formatMailMessage(new StringBuilder("[New Mail!]"), msg).toString();

            user.openPrivateChannel().queue(c -> {
                mailManager.splitContent(content).forEach(m -> c.sendMessage(m).queue(s -> {
                }, err -> {
                    var channelID = PostmanPatConfig.NotifyChannel;
                    var channel = jda.getTextChannelById(channelID);
                    if (channel == null) {
                        getLogger().info("Unable to find configured discord channel! " + channelID);
                        return;
                    }
                    channel.sendMessage(user.getAsMention() + " You have received mail! Check it with `/mail read` !").queue();
                }));
            });
        };


        Bukkit.getPluginManager().registerEvent(UserMailEvent.class, new Listener() {
        }, EventPriority.NORMAL, eventExecutor, this);
    }

    @Override
    public void onDisable() {

    }

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

    class DiscordMailManager {
        ArrayList<MailMessage> mailMessage;

        boolean includeRead;

        DiscordMailManager(ArrayList<MailMessage> mailMessage, boolean unread) {
            this.mailMessage = mailMessage;
            this.includeRead = unread;
        }

        DiscordMailManager(ArrayList<MailMessage> mailMessage) {
            this(mailMessage, false);
        }

        DiscordMailManager(MailMessage mailMessage) {
            this(new ArrayList<>(List.of(mailMessage)), false);
        }

        ArrayList<MailMessage> getMailMessage() {
            return mailMessage;
        }

        List<String> getPaginatedContent() {
            return splitContent(makeDisplayable(getMailMessage()));
        }

        String getPage(Long page) throws IndexOutOfBoundsException {
            return getPaginatedContent().get(page.intValue());
        }

        private String makeDisplayable(ArrayList<MailMessage> messages) {
            var builder = new StringBuilder();
            var msgSize = messages.size();
            IntStream.range(0, msgSize)
                    .forEach(i -> {
                        var m = messages.get(i);

                        if (includeRead || !m.isRead()) {
                            formatMailMessage(builder, m);
                        }
                    });

            return builder.toString();
        }

        private List<String> splitContent(String content) {
            List<String> message = new ArrayList<>();
            if (content.length() > MESSAGE_CONTENT_LIMIT) {
                message.add(content.substring(0, MESSAGE_CONTENT_LIMIT));
                var remains = content.substring(MESSAGE_CONTENT_LIMIT);
                if (remains.length() > MESSAGE_CONTENT_LIMIT) {
                    message.addAll(splitContent(remains));
                } else {
                    message.add(remains);
                }
            } else {
                message.add(content);
            }
            return appendPageNumbers(message);
        }

        private List<String> appendPageNumbers(List<String> splittedContent) {
            var size = splittedContent.size();
            List<String> paginatedList = new ArrayList<>();
            IntStream.range(0, size)
                    .forEach(i -> {
                        // Pages are 0 indexed, end users are 1 indexed
                        String content = splittedContent.get(i) + "\n[Page " + (i + 1) + "/" + (size) + "]";
                        paginatedList.add(content);
                    });
            return paginatedList;
        }

    }

    @SlashCommand(path = "mail/*")
    public void processSlashCommand(SlashCommandEvent event) {
        var sub = event.getSubcommandName();

        if (sub.contentEquals(PostmanPatConfig.readSubCommand)) {
            mailReadCommand(event);
        } else if (sub.contentEquals(PostmanPatConfig.sendSubCommand)) {
            mailSendCommand(event);
        } else if (sub.contentEquals(PostmanPatConfig.markReadSubCommand)) {
            markAsReadCommand(event);
        }

    }

    public void markAsReadCommand(SlashCommandEvent event) {
        var user = getEssxUser(event.getUser().getId());
        markMailAsRead(user, user.getMailMessages());
        event.reply("Mail has been marked as read!").setEphemeral(true).queue();
    }

    public void mailReadCommand(SlashCommandEvent event) {
        var user = getEssxUser(event.getUser().getId());
        var mailMessage = user.getMailMessages();

        var includeRead = event.getOption("include-read");
        var includeBool = includeRead != null && includeRead.getAsBoolean();

        if (!includeBool && user.getUnreadMailAmount() <= 0) {
            event.reply("No new mail!").setEphemeral(true).queue();
            return;
        }

        var mailManager = new DiscordMailManager(mailMessage, includeBool);
        var opt = event.getOption("page");
        String content;
        if (opt == null) {
            content = mailManager.getPage(0L);
        } else {
            var selPage = opt.getAsLong();
            try {
                content = mailManager.getPage(selPage - 1);
            } catch (IndexOutOfBoundsException e) {
                content = "No mail on page " + selPage;
            }
        }

        event.reply(content).setEphemeral(true).queue();


    }

    public void mailSendCommand(SlashCommandEvent event) {
        var user = Objects.requireNonNull(event.getOption("user")).getAsUser();
        var senderUser = event.getUser();

        var senderUUID = mgr().getUuid(senderUser.getId());
        var uuid = mgr().getUuid(user.getId());

        if (senderUUID == null || uuid == null) {
            event.reply("One or more users do not have a linked account!").setEphemeral(true).queue();
            return;
        }

        var essxUser = ess.getUser(uuid);
        var senderEssxUser = ess.getUser(senderUUID);

        var message = Objects.requireNonNull(event.getOption("message")).getAsString();

        essxUser.sendMail(senderEssxUser, message);

        event.reply("Sent mail to " + user.getAsTag())
                .setEphemeral(true).queue();
    }

    @ConfigMappable
    static class PostmanPatConfig {

        PostmanPatConfig() {
        }

        @Comment("The discord Channel ID to ping users if DMs are disabled")
        public static Long NotifyChannel = 0L;

        @Comment("root command for mail")
        public static String rootCommand = "mail";

        @Comment("Subcommand for reading mail")
        public static String readSubCommand = "read";

        @Comment("Subcommand for sending mail to another user")
        public static String sendSubCommand = "send";

        @Comment("Subcommand for marking all unread mail as read")
        public static String markReadSubCommand = "mark-read";

        @Comment("Argument for including mail that is already read")
        public static String includeReadArg = "include-read";

    }

    /**
     * @param id A discord user ID
     * @return An Essentials User
     */
    private User getEssxUser(String id) {
        return ess.getUser(mgr().getUuid(id));
    }

    private StringBuilder formatMailMessage(StringBuilder builder, MailMessage mail) {
        return builder.append("\nSent: ").append(Instant.ofEpochMilli(mail.getTimeSent()))
                .append("\nSender: ").append(mail.getSenderUsername())
                .append("\nMessage: ").append(mail.getMessage())
                .append("\n");
    }

    /**
     * Marks all mail as read in the Essentials mailbox
     *
     * @param user         an Essentials User
     * @param mailMessages The messages to send and mark as read
     */
    private void markMailAsRead(User user, ArrayList<MailMessage> mailMessages) {
        // Essx doesn't have an easy way to mark a single mail as read
        // So we have to re-create all the messages with the read bool
        // This mimics the way Essx does it in their `mail read` command
        ArrayList<MailMessage> readMail = mailMessages
                .stream()
                .map(m -> new MailMessage(true,
                        m.isLegacy(),
                        m.getSenderUsername(),
                        m.getSenderUUID(),
                        m.getTimeSent(),
                        m.getTimeExpire(),
                        m.getMessage())
                ).collect(Collectors.toCollection(ArrayList::new));
        user.setMailList(readMail);
    }

    AccountLinkManager mgr() {
        return srv.getAccountLinkManager();
    }

    JDA getJda() {
        return DiscordUtil.getJda();
    }
}

