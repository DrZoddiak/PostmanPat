package me.zodd.postmanpat

import com.earth2me.essentials.User
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent
import github.scarsz.discordsrv.objects.managers.AccountLinkManager
import me.zodd.postmanpat.PostmanPat.Companion.plugin
import java.util.*

object Utils {

    object EssxUtils {
        /**
         * @param id A discord user ID
         * @return An Essentials User
         */
        internal fun getEssxUser(id: String?): User? {
            return plugin.ess?.getUser(manager().getUuid(id))
        }

        /**
         * @param uuid A players user ID
         * @return An Essentials User
         */
        internal fun getEssxUser(uuid: UUID?): User? {
            return plugin.ess?.getUser(uuid)
        }

        /**
         * @param event SlashCommandEvent to get user from
         * @return An Essentials User
         */
        internal fun getEssxUser(event: SlashCommandEvent): User? {
            return getEssxUser(event.user.id)
        }

        /**
         * @return Account manager for DiscordSRV
         */
        internal fun manager(): AccountLinkManager {
            return plugin.srv.accountLinkManager
        }
    }

    /**
     * Utilities for Sending messages
     */
    object MessageUtils {
        /**
         * @return An ephemerally configured Embed, not yet queued.
         */
        fun SlashCommandEvent.replyEphemeralEmbed(embed: MessageEmbed, vararg embeds: MessageEmbed) =
            replyEmbeds(embed, *embeds).setEphemeral(true)

        /**
         * @return An ephemerally configured message, not yet queued.
         */
        fun SlashCommandEvent.replyEphemeral(message: String) = reply(message).setEphemeral(true)
    }

}