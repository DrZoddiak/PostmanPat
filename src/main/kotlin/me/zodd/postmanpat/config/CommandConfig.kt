package me.zodd.postmanpat.config

import me.zodd.postmanpat.econ.EconCommandConfig
import me.zodd.postmanpat.mail.MailCommandConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class CommandConfig(
    val mailCommands: MailCommandConfig = MailCommandConfig(),
    val econCommands: EconCommandConfig = EconCommandConfig()
)