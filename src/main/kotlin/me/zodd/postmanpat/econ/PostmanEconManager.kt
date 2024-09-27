package me.zodd.postmanpat.econ

import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent
import me.zodd.postmanpat.PostmanPat.Companion.plugin
import me.zodd.postmanpat.Utils.EssxUtils.getEssxUser
import me.zodd.postmanpat.Utils.MessageUtils.embedMessage
import me.zodd.postmanpat.Utils.MessageUtils.replyEphemeral
import me.zodd.postmanpat.econ.entity.EconEntity
import me.zodd.postmanpat.econ.entity.UserEntity

class PostmanEconManager(private val sender: EconEntity, private val event: SlashCommandEvent) {

    private val econConf = plugin.configManager.conf.moduleConfig.econ
    private val decimalFormat = econConf.decimalFormat()

    fun transferFunds(recipient: EconEntity) {
        val commandSender = getEssxUser(event)?: run {
            event.replyEphemeral("Unable to find user, please ensure your account is linked!").queue()
            return
        }

        takeUnless { sender.uuid == recipient.uuid } ?: run {
            PPEconomyTransactionResult.SENT_TO_SELF.emitError(event)
            return
        }

        takeIf { recipient.acceptingPayment.emitError(event).isSuccess() } ?: return

        val amount = checkAmountOption() ?: run {
            PPEconomyTransactionResult.UNDER_MINIMUM.emitError(event)
            return
        }


        takeIf { sender.hasEnough(amount).emitError(event).isSuccess() } ?: return

        when (val payment = sender.pay(UserEntity(commandSender), recipient, amount)) {
            PPEconomyTransactionResult.PLUGIN_WITHDRAW -> {
                payment.emitError(event, sender.name)
                return
            }

            PPEconomyTransactionResult.PLUGIN_DEPOSIT -> {
                payment.emitError(event, sender.name)
                takeIf { sender.deposit(amount).isSuccess() } ?: run {
                    plugin.logger.warning("Failed to revert transaction. ${sender.name} may be owed $amount")
                    PPEconomyTransactionResult.PLUGIN_REVERT_FAIL.emitError(event)
                }
                return
            }

            else -> {}
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






