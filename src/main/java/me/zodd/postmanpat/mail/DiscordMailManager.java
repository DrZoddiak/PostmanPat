package me.zodd.postmanpat.mail;


import me.zodd.postmanpat.PostmanPatConfig;
import net.essentialsx.api.v2.services.mail.MailMessage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class DiscordMailManager {
    ArrayList<MailMessage> mailMessage;

    boolean includeRead;

    DiscordMailManager(ArrayList<MailMessage> mailMessage, boolean unread) {
        this.mailMessage = mailMessage;
        this.includeRead = unread;
    }

    DiscordMailManager(ArrayList<MailMessage> mailMessage) {
        this(mailMessage, false);
    }

    public DiscordMailManager(MailMessage mailMessage) {
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

    public StringBuilder formatMailMessage(StringBuilder builder, MailMessage mail) {
        return builder.append("\nSent: ").append(Instant.ofEpochMilli(mail.getTimeSent()))
                .append("\nSender: ").append(mail.getSenderUsername())
                .append("\nMessage: ").append(mail.getMessage())
                .append("\n");
    }

    int maxSize = PostmanPatConfig.maxMessageSize;

    // Recursively splits content into chunks
    public List<String> splitContent(String content) {
        List<String> message = new ArrayList<>();
        if (content.length() > maxSize) {
            message.add(content.substring(0, maxSize));
            var remains = content.substring(maxSize);
            if (remains.length() > maxSize) {
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
