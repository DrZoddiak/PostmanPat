package me.zodd.postmanpat.econ

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
data class EconConfig(
    @field:Comment("Command for users to pay each other")
    val payCommand: String = "pay",
    @field:Comment("Command for users to check balances")
    val balCommand: String = "balance",
    @field:Comment("Minimum amount sendable")
    val minimumSendable: Double = 0.01,
    @field:Comment("The currency symbol to use.")
    val currencySymbol: String = "$",
    @field:Comment("The formating to use for displaying decimal numbers")
    val decimalFormat: String = "#,###.##",
    @field:Comment("The base command for business commands")
    val firmBaseCommand: String = "firm",
    @field:Comment("The sub command for issuing payments from businesses")
    val firmPayCommand: String = "pay"
)