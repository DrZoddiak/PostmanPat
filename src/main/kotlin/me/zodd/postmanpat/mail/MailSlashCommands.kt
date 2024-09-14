package me.zodd.postmanpat.mail

import com.earth2me.essentials.User
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent
import me.zodd.postmanpat.EssxData
import me.zodd.postmanpat.PostmanPat
import me.zodd.postmanpat.PostmanPat.Companion.configManager
import me.zodd.postmanpat.PostmanPat.Companion.userStorageManager
import net.essentialsx.api.v2.services.mail.MailMessage
import java.util.UUID


class MailSlashCommands(private val plugin: PostmanPat) : EssxData(plugin) {
    fun ignoreUserCommand(event: SlashCommandEvent) {
        val user = getEssxUser(event) ?: run {
            event.reply("Unable to find Minecraft User!").setEphemeral(true).queue()
            return
        }

        val userOpt = event.getOption("user")
        val uuidOpt = event.getOption("uuid")

        if (userOpt == null && uuidOpt == null) {
            event.reply("You may target a user by their @, or by a players UUID!").setEphemeral(true).queue()
            return
        }


        val targetUUID = if (userOpt != null) {
            getEssxUser(userOpt.asUser.id)?.uuid
        } else {
            UUID.fromString(uuidOpt?.asString)
        } ?: run {
            event.reply("UUID malformed").setEphemeral(true).queue()
            return
        }

        val userList: MutableList<UUID> =
            userStorageManager.conf.mailIgnoreList.getOrDefault(user?.uuid, mutableListOf())

        val targetUser = getEssxUser(targetUUID)
        val targetName = if (targetUser == null) {
            targetUUID.toString()
        } else {
            targetUser.name
        }

        if (userList.remove(targetUUID)) {
            event.reply("You have removed $targetName to your ignore list.")
                .setEphemeral(true)
                .queue()
        } else {
            userList.add(targetUUID)
            event.reply("You have added $targetName to your ignore list.")
                .setEphemeral(true)
                .queue()
        }
        userStorageManager.conf.mailIgnoreList[user.uuid] = userList
        userStorageManager.save()
    }

    fun markAsReadCommand(event: SlashCommandEvent) {
        val user = getEssxUser(event.user.id) ?: run {
            event.reply("Unable to find user, is your account linked?").setEphemeral(true).queue()
            return
        }
        markMailAsRead(user, user.mailMessages)
        event.reply("Mail has been marked as read!").setEphemeral(true).queue()
    }

    fun mailReadCommand(event: SlashCommandEvent) {
        val user = getEssxUser(event.user.id) ?: run {
            event.reply("Unable to find user, is your account linked?").setEphemeral(true).queue()
            return
        }
        val mailMessage = user.mailMessages

        val includeRead = event.getOption(configManager.conf.commandConfig.mailCommands.includeReadArg)
        val includeBool = includeRead != null && includeRead.asBoolean

        if (!includeBool && user.unreadMailAmount <= 0) {
            event.reply("No new mail!").setEphemeral(true).queue()
            return
        }

        val mailManager = DiscordMailManager(mailMessage, includeBool)
        val opt = event.getOption("page")
        val content: String = if (opt == null) {
            mailManager.getPage(0L)
        } else {
            val selPage = opt.asLong
            try {
                mailManager.getPage(selPage - 1)
            } catch (e: IndexOutOfBoundsException) {
                "No mail on page $selPage"
            }
        }

        event.reply(content).setEphemeral(true).queue()
    }

    fun mailSendCommand(event: SlashCommandEvent) {
        val user = event.getOption("user")?.asUser ?: run {
            event.reply("Could not find user!").setEphemeral(true).queue()
            return
        }
        val senderUser = event.user

        val senderUUID = mgr().getUuid(senderUser.id)
        val uuid = mgr().getUuid(user.id)

        if (senderUUID == null || uuid == null) {
            event.reply("One or more users do not have a linked account!").setEphemeral(true).queue()
            return
        }

        val essxUser = plugin.ess?.getUser(uuid)
        val senderEssxUser = plugin.ess?.getUser(senderUUID)

        val message = event.getOption("message")?.asString

        essxUser?.sendMail(senderEssxUser, message)

        event.reply("Sent mail to " + user.asTag)
            .setEphemeral(true).queue()
    }

    /**
     * Marks all mail as read in the Essentials mailbox
     *
     * @param user         an Essentials User
     * @param mailMessages The messages to send and mark as read
     */
    private fun markMailAsRead(user: User?, mailMessages: List<MailMessage>) {
        // Essx doesn't have an easy way to mark a single mail as read
        // So we have to re-create all the messages with the read bool
        // This mimics the way Essx does it in their `mail read` command
        val readMail = mailMessages
            .map { m: MailMessage ->
                MailMessage(
                    true,
                    m.isLegacy,
                    m.senderUsername,
                    m.senderUUID,
                    m.timeSent,
                    m.timeExpire,
                    m.message
                )
            }.toCollection(arrayListOf())
        user?.setMailList(readMail)
    }
}
