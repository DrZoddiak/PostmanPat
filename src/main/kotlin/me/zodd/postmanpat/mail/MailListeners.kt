package me.zodd.postmanpat.mail

import github.scarsz.discordsrv.dependencies.jda.api.entities.PrivateChannel
import me.zodd.postmanpat.PostmanPat
import me.zodd.postmanpat.Utils.EssxUtils.getEssxUser
import me.zodd.postmanpat.Utils.EssxUtils.manager
import net.essentialsx.api.v2.events.UserMailEvent
import org.bukkit.event.Event
import org.bukkit.event.Listener
import java.time.Instant

class MailListeners(private var plugin: PostmanPat) {

    fun userMailListener(exec: Listener?, e: Event) {
        val event = e as UserMailEvent

        val msg = event.message
        val senderUUID = msg.senderUUID
        val senderUser = getEssxUser(senderUUID)

        val mailManager = DiscordMailManager(listOf(msg))
        val recipient = event.recipient
        val discordID = manager().getDiscordId(recipient.uuid)

        if (discordID == null) {
            plugin.logger.warning("Failed to retrieve discord ID, account may not be linked.")
            return
        }

        val user = plugin.jda.getUserById(discordID) ?: run {
            plugin.logger.warning("Failed to get user by ID, are they in the guild?")
            return
        }

        val ignores = plugin.userStorageManager.conf.mailIgnoreList.getOrDefault(recipient.uuid, ArrayList())

        if (getEssxUser(user.id)?.isIgnoredPlayer(senderUser) == true || (ignores.isNotEmpty() && ignores.contains(
                senderUUID
            ))
        ) {
            // Don't send a message if ignoring player
            // They will still receive the mail, just not through discord
            return
        }

        val content = """
                Sent: ${Instant.ofEpochMilli(msg.timeSent)}
                Sender: ${msg.senderUsername}
                Message: ${msg.message.replace("§\\w".toRegex(), "")}
                
                """.trimIndent()

        user.openPrivateChannel().queue { c: PrivateChannel ->
            mailManager.splitContent(content).forEach { m: String ->
                c.sendMessage(m).queue(
                    { }
                ) OnFail@{
                    // If we're unable to send a DM to the user
                    val channelID = plugin.configManager.conf.moduleConfig.mail.notificationChannel
                    val channel =
                        plugin.jda.getTextChannelById(channelID)
                    if (channel == null) {
                        plugin.logger.warning("Unable to find configured discord channel! $channelID")
                        return@OnFail
                    }
                    channel.sendMessage(user.asMention + " You have received mail! Check it with `/mail read`!")
                        .queue()
                }
            }
        }
    }
}