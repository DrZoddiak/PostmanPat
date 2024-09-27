package me.zodd.postmanpat.econ

import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent
import me.zodd.postmanpat.PostmanPat.Companion.plugin
import me.zodd.postmanpat.Utils.MessageUtils.replyEphemeral

enum class PPEconomyTransactionResult(private val msg: String) {
    INSUFFICIENT_FUNDS("You are too poor for this transaction!"),
    UNDER_MINIMUM("Amount must be more than ${plugin.configManager.conf.moduleConfig.econ.minimumSendable}!"),
    SENT_TO_SELF("You cannot send money to yourself!"),
    PLUGIN_WITHDRAW("Failed to withdraw money from {}."),
    PLUGIN_DEPOSIT("Failed to deposit money to {}, transaction reverted."),
    PLUGIN_REVERT_FAIL("Failed to revert transaction. Please contact an admin."),
    NOT_ACCEPTING_PAY("Target user is not accepting payments at this time!"),
    SUCCESS("Success"); // Message won't be sent

    fun isSuccess() = when (this) {
        SUCCESS -> true
        else -> false
    }

    fun emitError(event: SlashCommandEvent, arg: String? = null): PPEconomyTransactionResult {
        if (!isSuccess())
            event.replyEphemeral(msg.replace("{}", "$arg")).queue()
        return this
    }
}