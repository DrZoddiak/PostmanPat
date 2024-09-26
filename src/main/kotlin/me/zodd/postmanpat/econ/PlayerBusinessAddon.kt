package me.zodd.postmanpat.econ

import com.earth2me.essentials.User
import com.olziedev.playerbusinesses.api.PlayerBusinessesAPI
import com.olziedev.playerbusinesses.api.business.BStaff
import com.olziedev.playerbusinesses.api.business.Business
import com.olziedev.playerbusinesses.api.business.BusinessPermission
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent
import me.zodd.postmanpat.PostmanPat
import me.zodd.postmanpat.Utils.EssxUtils.getEssxUser
import me.zodd.postmanpat.Utils.MessageUtils.embedMessage
import me.zodd.postmanpat.Utils.SlashCommandUtils.get
import me.zodd.postmanpat.Utils.MessageUtils.replyEphemeral
import me.zodd.postmanpat.Utils.MessageUtils.replyEphemeralEmbed
import me.zodd.postmanpat.econ.entity.BusinessEntity
import me.zodd.postmanpat.econ.entity.UserEntity
import java.awt.Color

class PlayerBusinessAddon {

    private val pba: PlayerBusinessesAPI by lazy {
        PlayerBusinessesAPI.getInstance()
    }

    private val econConf = PostmanPat.plugin.configManager.conf.moduleConfig.econ
    private val decimalFormat = econConf.decimalFormat()

    internal fun listOwnedBusinesses(event: SlashCommandEvent) {
        val senderUser = getEssxUser(event) ?: run {
            event.replyEphemeral("Unable to find User, account may not be linked!")
                .queue()
            return
        }
        val embedBuilder = EmbedBuilder()
            .setTitle("Owned Businesses")
            .setColor(Color.blue)
            .setFooter(PostmanPat.plugin.configManager.conf.serverBranding)
        pba.getBusinessesByPlayer(senderUser.uuid).map { it.name }.chunked(25).map {


        //embedBuilder.addField(it, "", true)
        }

        event.replyEphemeralEmbed(
            embedBuilder.build()
        ).queue()
    }

    internal fun firmBal(event: SlashCommandEvent) {
        val senderUser = getEssxUser(event) ?: run {
            event.replyEphemeral("Unable to find User, account may not be linked!")
                .queue()
            return
        }
        val businessName = event["business"]?.asString
        val business: Business = pba.getBusinessByName(businessName?.lowercase()) ?: run {
            event.replyEphemeral("Business by name [$businessName] was not found!").queue()
            return
        }
        senderUser.hasFirmPermission(event, business) ?: return
        event.replyEphemeralEmbed(
            embedMessage(
                "Balance for ${business.name}",
                "${econConf.currencySymbol}${decimalFormat.format(business.balance)}"
            )
        ).queue()
    }


    internal fun firmPay(event: SlashCommandEvent) {

        val senderUser = getEssxUser(event) ?: run {
            event.replyEphemeral("Unable to find User, account may not be linked!")
                .queue()
            return
        }

        val businessName = event["business"]?.asString

        val targetUser = getEssxUser(event["user"]?.asUser?.id) ?: run {
            event.replyEphemeral("Unable to find User, account may not be linked!")
                .queue()
            return
        }

        val business: Business = pba.getBusinessByName(businessName?.lowercase()) ?: run {
            event.replyEphemeral("Business by name [$businessName] was not found!").queue()
            return
        }

        senderUser.hasFirmPermission(event, business) ?: return

        val sender = BusinessEntity(business)
        val receiver = UserEntity(targetUser)
        PostmanEconManager(sender, event).transferFunds(receiver)
    }

    private fun User.hasFirmPermission(event: SlashCommandEvent, business: Business): BStaff? {
        return business.staff?.firstOrNull {
            val permCheck = it.role.permission
            it.uuid == uuid && (permCheck.contains(BusinessPermission.FINANCIAL)
                    || permCheck.contains(BusinessPermission.PROPRIETOR)
                    || permCheck.contains(BusinessPermission.ADMINISTRATOR))
        } ?: run {
            event.replyEphemeral("You do not have permission to transfer funds from this business")
                .queue()
            null
        }
    }

}