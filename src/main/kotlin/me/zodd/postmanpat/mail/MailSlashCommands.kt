package me.zodd.postmanpat.mail

import com.earth2me.essentials.User
import github.scarsz.discordsrv.api.commands.PluginSlashCommand
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.SubcommandData
import me.zodd.postmanpat.PostmanPat.Companion.plugin
import me.zodd.postmanpat.Utils.EssxUtils.getEssxUser
import me.zodd.postmanpat.Utils.EssxUtils.manager
import me.zodd.postmanpat.Utils.MessageUtils.replyEphemeralMessage
import me.zodd.postmanpat.command.PPSlashCommand
import me.zodd.postmanpat.command.PostmanCommandProvider
import net.essentialsx.api.v2.services.mail.MailMessage
import java.util.UUID

class MailSlashCommands : PostmanCommandProvider {

    enum class MailCommands(override val command: String) : PPSlashCommand<MailCommands> {
        MAIL_BASE(plugin.configManager.conf.moduleConfig.mail.baseCommand),
        MAIL_READ(plugin.configManager.conf.moduleConfig.mail.readSubCommand),
        MAIL_SEND(plugin.configManager.conf.moduleConfig.mail.sendSubCommand),
        MAIL_MARK_READ(plugin.configManager.conf.moduleConfig.mail.markReadSubCommand),
        MAIL_IGNORE(plugin.configManager.conf.moduleConfig.mail.ignoreSubCommand);

        override fun exec(): (SlashCommandEvent) -> Unit {
            return when (this) {
                MAIL_READ -> this::mailReadCommand
                MAIL_SEND -> this::mailSendCommand
                MAIL_MARK_READ -> this::markAsReadCommand
                MAIL_IGNORE -> this::ignoreUserCommand
                MAIL_BASE -> { _ -> /*This command is never run*/ }
            }
        }

        private fun ignoreUserCommand(event: SlashCommandEvent) {
            val user = getEssxUser(event) ?: run {
                event.replyEphemeralMessage("Unable to find Minecraft User!").queue()
                return
            }

            val userOpt = event.getOption("user")
            val uuidOpt = event.getOption("uuid")

            if (userOpt == null && uuidOpt == null) {
                event.replyEphemeralMessage("You may target a user by their @, or by a players UUID!").queue()
                return
            }

            val targetUUID = if (userOpt != null) {
                getEssxUser(userOpt.asUser.id)?.uuid
            } else {
                UUID.fromString(uuidOpt?.asString)
            } ?: run {
                event.replyEphemeralMessage("UUID malformed").queue()
                return
            }

            val userList: MutableList<UUID> =
                plugin.userStorageManager.conf.mailIgnoreList.getOrDefault(user.uuid, mutableListOf())

            val targetUser = getEssxUser(targetUUID)
            val targetName = if (targetUser == null) {
                targetUUID.toString()
            } else {
                targetUser.name
            }

            if (userList.remove(targetUUID)) {
                event.replyEphemeralMessage("You have removed $targetName to your ignore list.")
                    .queue()
            } else {
                userList.add(targetUUID)
                event.replyEphemeralMessage("You have added $targetName to your ignore list.")
                    .queue()
            }
            plugin.userStorageManager.conf.mailIgnoreList[user.uuid] = userList
            plugin.userStorageManager.save()
        }

        private fun markAsReadCommand(event: SlashCommandEvent) {
            val user = getEssxUser(event.user.id) ?: run {
                event.reply("Unable to find user, is your account linked?").setEphemeral(true).queue()
                return
            }
            markMailAsRead(user, user.mailMessages)
            event.reply("Mail has been marked as read!").setEphemeral(true).queue()
        }

        private fun mailReadCommand(event: SlashCommandEvent) {
            val user = getEssxUser(event.user.id) ?: run {
                event.replyEphemeralMessage("Unable to find user, is your account linked?").queue()
                return
            }
            val mailMessage = user.mailMessages

            val includeRead = event.getOption(plugin.configManager.conf.moduleConfig.mail.includeReadArg)
            val includeBool = includeRead != null && includeRead.asBoolean

            if (!includeBool && user.unreadMailAmount <= 0) {
                event.replyEphemeralMessage("No new mail!").queue()
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

            event.replyEphemeralMessage(content).queue()
        }

        private fun mailSendCommand(event: SlashCommandEvent) {
            val user = event.getOption("user")?.asUser ?: run {
                event.replyEphemeralMessage("Could not find user!").queue()
                return
            }
            val senderUser = event.user

            val senderUUID = manager().getUuid(senderUser.id)
            val uuid = manager().getUuid(user.id)

            if (senderUUID == null || uuid == null) {
                event.replyEphemeralMessage("One or more users do not have a linked account!").queue()
                return
            }

            val essxUser = plugin.ess?.getUser(uuid)
            val senderEssxUser = plugin.ess?.getUser(senderUUID)

            val message = event.getOption("message")?.asString

            essxUser?.sendMail(senderEssxUser, message)

            event.replyEphemeralMessage("Sent mail to " + user.asTag).queue()
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

    override fun slashCommands(): List<PluginSlashCommand> {
        return listOf(
            PluginSlashCommand(
                plugin, CommandData(MailCommands.MAIL_BASE.command, "mail command")
                    .addSubcommands(
                        SubcommandData(MailCommands.MAIL_READ.command, "read mail")
                            .addOption(OptionType.INTEGER, "page", "page number to view", false)
                            .addOption(
                                OptionType.BOOLEAN,
                                plugin.configManager.conf.moduleConfig.mail.includeReadArg,
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
            )
        )
    }
}

