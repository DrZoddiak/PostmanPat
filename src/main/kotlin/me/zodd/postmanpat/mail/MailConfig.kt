package me.zodd.postmanpat.mail

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
data class MailConfig(
    @field:Comment("Base command for mail")
    val baseCommand: String = "mail",
    @field:Comment("Sub command for viewing mail")
    val readSubCommand: String = "read",
    @field:Comment("Command flag for viewing mail that has already been marked as read")
    val includeReadArg: String = "include-read",
    @field:Comment("Sub command for sending mail to another user")
    val sendSubCommand: String = "send",
    @field:Comment("Sub command to mark existing mail as read")
    val markReadSubCommand: String = "mark-read",
    @field:Comment("Sub command to ")
    val ignoreSubCommand: String = "ignore",
    @field:Comment("The discord Channel ID to ping users if DMs are disabled")
    val notificationChannel: Long = 0L,
    @field:Comment(
        """Maximum characters a message can be. Discord sets a limit at 2000.
        Default value is set to 1900 to allow for some additional meta-data
        It is suggested you do not raise this value above 1900, lest ye risk errors"""
    )
    val maxMessageSize: Int = 1900,
)