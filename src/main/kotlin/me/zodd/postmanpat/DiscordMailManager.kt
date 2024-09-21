package me.zodd.postmanpat

import me.zodd.postmanpat.PostmanPat.Companion.plugin
import net.essentialsx.api.v2.services.mail.MailMessage
import java.time.Instant

class DiscordMailManager internal constructor(
    private val mailMessage: List<MailMessage>,
    private val includeRead: Boolean = false
) {

    private val paginatedContent: List<String>
        get() = splitContent(makeDisplayable(mailMessage))

    @Throws(IndexOutOfBoundsException::class)
    fun getPage(page: Long): String {
        return paginatedContent[page.toInt()]
    }

    private fun makeDisplayable(messages: List<MailMessage>): String {
        return messages.joinToString("\n") {
            if (includeRead || !it.isRead) {
                """
                Sent: ${Instant.ofEpochMilli(it.timeSent)}
                Sender: ${it.senderUsername}
                Message: ${it.message.replace("§\\w".toRegex(), "")}
                
                """.trimIndent()
            } else ""
        }
    }

    private val maxSize: Int = plugin.configManager.conf.moduleConfig.mail.maxMessageSize

    fun splitContent(content: String): List<String> {
        return appendPageNumbers(content.chunked(maxSize))
    }

    private fun appendPageNumbers(splittedContent: List<String>): List<String> {
        return splittedContent.mapIndexed { i, s ->
            """
                $s
                [Page ${i + 1}/${splittedContent.size}]
            """.trimIndent()
        }
    }
}
