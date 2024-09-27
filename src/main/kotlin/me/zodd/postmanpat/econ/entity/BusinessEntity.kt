package me.zodd.postmanpat.econ.entity

import com.olziedev.playerbusinesses.api.business.Business
import java.time.Instant
import java.util.UUID

class BusinessEntity(private val business: Business) : EconEntity {

    override val uuid: UUID = business.uuid

    override val name: String = business.name

    override val acceptingPayment: PPEconomyTransactionResult = PPEconomyTransactionResult.SUCCESS

    override val balance: Double
        get() = business.balance

    override fun deposit(amount: Double): PPEconomyTransactionResult {
        // I have no way of knowing if this worked or not, so it always does!
        business.balance += amount
        return PPEconomyTransactionResult.SUCCESS
    }

    override fun pay(sender: UserEntity, econEntity: EconEntity, amount: Double): PPEconomyTransactionResult {
        econEntity.withdraw(amount)
        business.transactions.addPaidEntry(sender.uuid, econEntity.uuid, amount, Instant.now().toEpochMilli())
        return deposit(amount)
    }

    override fun withdraw(amount: Double): PPEconomyTransactionResult {
        // I have no way of knowing if this worked or not, so it always does!
        business.balance -= amount
        return PPEconomyTransactionResult.SUCCESS
    }
}