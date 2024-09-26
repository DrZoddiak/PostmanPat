package me.zodd.postmanpat.econ

import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent
import me.zodd.postmanpat.PostmanPat.Companion.plugin
import me.zodd.postmanpat.Utils.MessageUtils.embedMessage
import me.zodd.postmanpat.econ.entity.EconEntity
import me.zodd.postmanpat.econ.entity.PPEconomyTransactionResult

class PostmanEconManager(private val sender: EconEntity, private val event: SlashCommandEvent) {

    private val econConf = plugin.configManager.conf.moduleConfig.econ
    private val decimalFormat = econConf.decimalFormat()

    fun transferFunds(recipient: EconEntity) {
        takeUnless { sender.uuid == recipient.uuid } ?: run {
            PPEconomyTransactionResult.SENT_TO_SELF.emitError(event)
            return
        }

        takeIf { sender.acceptingPayment.emitError(event).isSuccess() } ?: return

        val amount = checkAmountOption() ?: run {
            PPEconomyTransactionResult.UNDER_MINIMUM.emitError(event)
            return
        }

        takeIf { sender.hasEnough(amount).emitError(event).isSuccess() } ?: return

        takeIf { sender.withdraw(amount).emitError(event, sender.name).isSuccess() } ?: return

        takeIf { recipient.deposit(amount).emitError(event, recipient.name).isSuccess() } ?: run {
            //Revert on fail
            takeIf { sender.deposit(amount).isSuccess() } ?: run {
                plugin.logger.warning("Failed to revert transaction. ${sender.name} may be owed $amount")
                PPEconomyTransactionResult.PLUGIN_REVERT_FAIL.emitError(event)
                return
            }
            return
        }

        event.replyEmbeds(
            embedMessage(
                "${sender.name} -> ${recipient.name}",
                "You have sent ${econConf.currencySymbol}${decimalFormat.format(amount)} to ${recipient.name}"
            )
        ).queue()
    }

    private fun isAtleastMinimum(amount: Double): Boolean {
        return amount >= plugin.configManager.conf.moduleConfig.econ.minimumSendable
    }

    private fun checkAmountOption(): Double? {
        return event.getOption("amount")?.asDouble?.takeIf { isAtleastMinimum(it) }
    }
}






