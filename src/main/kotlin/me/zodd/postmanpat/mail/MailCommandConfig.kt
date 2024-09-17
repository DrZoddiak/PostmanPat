package me.zodd.postmanpat.mail

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
data class MailCommandConfig(
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
)