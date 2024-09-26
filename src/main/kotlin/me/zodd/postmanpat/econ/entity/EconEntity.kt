package me.zodd.postmanpat.econ.entity

import java.util.UUID

/**
 * Represents an Entity that has Economy Capabilities
 */
interface EconEntity {
    /**
     * UUID of a given Entity
     */
    val uuid: UUID

    /**
     * The name of the entity
     */
    val name: String

    /**
     * The Entities current balance
     */
    val balance: Double

    /**
     * Whether the entity is accepting payment
     */
    val acceptingPayment: PPEconomyTransactionResult

    /**
     * @param amount the amount to deposit into Entities account
     */
    fun deposit(amount: Double): PPEconomyTransactionResult

    /**
     * @param amount the amount to withdraw from Entities account
     */
    fun withdraw(amount: Double): PPEconomyTransactionResult

    /**
     * @param amount the amount to check against Entities balance
     * @return true if account has enough funds for transaction
     */
    fun hasEnough(amount: Double) = takeIf { balance >= amount }?.let { PPEconomyTransactionResult.SUCCESS }
        ?: PPEconomyTransactionResult.INSUFFICIENT_FUNDS

}