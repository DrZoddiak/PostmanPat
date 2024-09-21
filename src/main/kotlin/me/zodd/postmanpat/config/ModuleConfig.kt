package me.zodd.postmanpat.config

import me.zodd.postmanpat.econ.EconConfig
import me.zodd.postmanpat.MailConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class ModuleConfig(
    val mail: MailConfig = MailConfig(),
    val econ: EconConfig = EconConfig()
)