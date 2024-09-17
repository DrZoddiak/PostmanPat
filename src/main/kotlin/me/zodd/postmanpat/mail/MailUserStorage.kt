package me.zodd.postmanpat.mail

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.util.UUID

@ConfigSerializable
data class MailUserStorage(
    val mailIgnoreList: MutableMap<UUID, MutableList<UUID>> = mutableMapOf()
)
