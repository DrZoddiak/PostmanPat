package me.zodd.postmanpat.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
data class PostmanPatConfig(
    @field:Comment("The discord Channel ID to ping users if DMs are disabled")
    val notificationChannel: Long = 0L,
    @field:Comment(
        """Maximum characters a message can be. Discord sets a limit at 2000.
        Default value is set to 1900 to allow for some additional meta-data
        It is suggested you do not raise this value above 1900, lest ye risk errors"""
    )
    val maxMessageSize: Int = 1900,
    @field:Comment("Will appear at the footer of embeds")
    val serverBranding: String = "Powered by PostmanPat",
    val commandConfig: CommandConfig = CommandConfig(),
)

