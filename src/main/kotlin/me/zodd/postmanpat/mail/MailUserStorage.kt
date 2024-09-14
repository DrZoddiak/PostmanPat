package me.zodd.postmanpat.mail

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting
import java.util.UUID

@ConfigSerializable
data class MailUserStorage(
    @field:Setting
    val mailIgnoreList: MutableMap<UUID, MutableList<UUID>> = mutableMapOf()
)
