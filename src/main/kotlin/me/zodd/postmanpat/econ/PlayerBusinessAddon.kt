package me.zodd.postmanpat.econ

import com.olziedev.playerbusinesses.api.PlayerBusinessesAPI
import com.olziedev.playerbusinesses.api.business.Business
import com.olziedev.playerbusinesses.api.business.BusinessPermission
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent
import me.zodd.postmanpat.EssxUtils.getEssxUser
import me.zodd.postmanpat.PostmanPat.Companion.plugin
import java.awt.Color
import java.text.DecimalFormat

class PlayerBusinessAddon {

    private val pba: PlayerBusinessesAPI by lazy {
        PlayerBusinessesAPI.getInstance()
    }

    private val econConfig = plugin.configManager.conf.commandConfig.econCommands
    private val decimalFormat = DecimalFormat(econConfig.decimalFormat)

    internal val businesses: List<Business> get() = pba.businesses

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

        val amount = event.getOption("amount")?.asDouble ?: run {
            event.reply("Invalid amount")
                .setEphemeral(true)
                .queue()
            return
        }

        business.balance -= amount

        plugin.econ?.depositPlayer(plugin.server.getOfflinePlayer(targetUser.uuid), amount)
            ?.takeIf { it.transactionSuccess() } ?: run {
            business.balance += amount

            event.reply("Transaction failed!")
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

        event.replyEmbeds(embed)
            .setEphemeral(true)
            .queue()
    }

}