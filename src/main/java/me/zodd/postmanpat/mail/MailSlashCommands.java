package me.zodd.postmanpat.mail;

import com.earth2me.essentials.IEssentials;
import com.earth2me.essentials.User;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import github.scarsz.discordsrv.objects.managers.AccountLinkManager;
import net.essentialsx.api.v2.services.mail.MailMessage;

import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

public class MailSlashCommands {

    IEssentials ess;
    DiscordSRV srv;

    public MailSlashCommands(IEssentials ess, DiscordSRV srv) {
        this.ess = ess;
        this.srv = srv;
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

    /**
     * @param id A discord user ID
     * @return An Essentials User
     */
    private User getEssxUser(String id) {
        return ess.getUser(mgr().getUuid(id));
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

}
