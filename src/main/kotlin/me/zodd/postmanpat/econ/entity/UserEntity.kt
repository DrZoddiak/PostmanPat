package me.zodd.postmanpat.econ.entity

import com.earth2me.essentials.User
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent
import me.zodd.postmanpat.PostmanPat.Companion.plugin
import me.zodd.postmanpat.Utils.MessageUtils.replyEphemeral
import org.bukkit.OfflinePlayer
import java.util.UUID

class UserEntity(user: User) : EconEntity {

    override val uuid: UUID = user.uuid

    override val name: String = user.name

    private val offlinePlayer: OfflinePlayer
        get() = plugin.server.getOfflinePlayer(uuid)

    override val balance: Double
        get() = plugin.econ.getBalance(offlinePlayer)

    override val acceptingPayment: PPEconomyTransactionResult =
        if (user.isAcceptingPay) PPEconomyTransactionResult.SUCCESS else PPEconomyTransactionResult.NOT_ACCEPTING_PAY

    override fun deposit(amount: Double): PPEconomyTransactionResult {
        takeIf { plugin.econ.depositPlayer(offlinePlayer, amount).transactionSuccess() }
            ?: return PPEconomyTransactionResult.PLUGIN_DEPOSIT
        return PPEconomyTransactionResult.SUCCESS
    }

    override fun withdraw(amount: Double): PPEconomyTransactionResult {
        takeIf { plugin.econ.withdrawPlayer(offlinePlayer, amount).transactionSuccess() }
            ?: return PPEconomyTransactionResult.PLUGIN_WITHDRAW
        return PPEconomyTransactionResult.SUCCESS
    }
}

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
            event.replyEphemeral(msg.replace("{}","$arg")).queue()
        return this
    }
}