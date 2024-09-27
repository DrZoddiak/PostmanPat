package me.zodd.postmanpat.econ.entity

import com.earth2me.essentials.User
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent
import me.zodd.postmanpat.PostmanPat.Companion.plugin
import me.zodd.postmanpat.Utils.MessageUtils.replyEphemeral
import me.zodd.postmanpat.econ.PPEconomyTransactionResult
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

    override fun pay(sender: UserEntity, econEntity: EconEntity, amount: Double): PPEconomyTransactionResult {
        val res = econEntity.withdraw(amount)
        takeIf { res.isSuccess() } ?: return res // Return error
        return deposit(amount)
    }
}

