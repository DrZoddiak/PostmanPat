package me.zodd.postmanpat.econ

import com.olziedev.playerbusinesses.api.PlayerBusinessesAPI
import com.olziedev.playerbusinesses.api.business.Business
import com.olziedev.playerbusinesses.api.business.BusinessPermission
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent
import me.zodd.postmanpat.PostmanPat.Companion.plugin
import me.zodd.postmanpat.Utils.EssxUtils.getEssxUser
import java.awt.Color
import java.text.DecimalFormat

class PlayerBusinessAddon {

    private val pba: PlayerBusinessesAPI by lazy {
        PlayerBusinessesAPI.getInstance()
    }

    private val econConfig = plugin.configManager.conf.moduleConfig.econ
    private val decimalFormat = DecimalFormat(econConfig.decimalFormat)

    internal val businesses: List<Business> get() = pba.businesses

    fun isAtleastMinimum(amount: Double): Boolean {
        return amount >= econConfig.minimumSendable
    }

    fun checkAmountOption(event: SlashCommandEvent): Double? {
        return event.getOption("amount")?.asDouble?.takeIf { isAtleastMinimum(it) }
    }

    internal fun firmPay(event: SlashCommandEvent) {
        val senderUser = getEssxUser(event) ?: run {
            event.reply("Unable to find User, account may not be linked!")
                .setEphemeral(true)
                .queue()
            return
        }

        val businessName = event.getOption("business")?.asString

        val targetUser = getEssxUser(event.getOption("user")?.asUser?.id) ?: run {
            event.reply("Unable to find User, account may not be linked!")
                .setEphemeral(true)
                .queue()
            return
        }

        val business = pba.getBusinessByName(businessName)

        business?.staff?.firstOrNull {
            val permCheck = it.role.permission
            val check = permCheck.contains(BusinessPermission.FINANCIAL)
                    || permCheck.contains(BusinessPermission.PROPRIETOR)
                    || permCheck.contains(BusinessPermission.ADMINISTRATOR)
            it.uuid == senderUser.uuid && check
        } ?: run {
            event.reply("You do not have permission to transfer funds from this business")
                .setEphemeral(true)
                .queue()
            return
        }

        val amount = checkAmountOption(event) ?: run {
            event.reply("Amount provided was invalid, must be mat least ${econConfig.minimumSendable}")
                .setEphemeral(true)
                .queue()
            return
        }

        business.balance -= amount

        plugin.econ?.depositPlayer(plugin.server.getOfflinePlayer(targetUser.uuid), amount)
            ?.takeIf { it.transactionSuccess() } ?: run {
            business.balance += amount

            event.reply("Transaction failed, withdraw reverted.")
                .setEphemeral(true)
                .queue()
            return
        }

        val embed = EmbedBuilder().apply {
            setTitle("Payment: $businessName -> ${targetUser.name}")
            setColor(Color.GREEN)
            setDescription("You have sent ${econConfig.currencySymbol}${decimalFormat.format(amount)} to ${targetUser.name}")
            setFooter(plugin.configManager.conf.serverBranding)
        }.build()

        event.replyEphemeralEmbed(embed).queue()
    }

    fun SlashCommandEvent.replyEphemeralEmbed(embed: MessageEmbed, vararg embeds: MessageEmbed) =
        this.replyEmbeds(embed, *embeds).setEphemeral(true)
}