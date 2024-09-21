package me.zodd.postmanpat

import com.earth2me.essentials.User
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent
import github.scarsz.discordsrv.objects.managers.AccountLinkManager
import me.zodd.postmanpat.PostmanPat.Companion.plugin
import java.util.*

object EssxUtils {
    /**
     * @param id A discord user ID
     * @return An Essentials User
     */
    internal fun getEssxUser(id: String?): User? {
        return plugin.ess?.getUser(mgr().getUuid(id))
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

    internal fun mgr(): AccountLinkManager {
        return plugin.srv.accountLinkManager
    }
}