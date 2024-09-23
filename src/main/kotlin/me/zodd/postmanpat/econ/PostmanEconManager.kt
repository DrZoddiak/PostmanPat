package me.zodd.postmanpat.econ

import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent
import me.zodd.postmanpat.PostmanPat.Companion.plugin

/*
 * todo: Migrate this to a class that manages accounts
*/
object PostmanEconManager {

    private fun isAtleastMinimum(amount: Double): Boolean {
        return amount >= plugin.configManager.conf.moduleConfig.econ.minimumSendable
    }

    internal fun checkAmountOption(event: SlashCommandEvent): Double? {
        return event.getOption("amount")?.asDouble?.takeIf { isAtleastMinimum(it) }
    }
}