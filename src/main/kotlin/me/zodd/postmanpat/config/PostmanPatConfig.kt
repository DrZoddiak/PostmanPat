package me.zodd.postmanpat.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
data class PostmanPatConfig(
    @field:Comment("Will appear at the footer of embeds")
    val serverBranding: String = "Powered by PostmanPat",
    val moduleConfig: ModuleConfig = ModuleConfig(),
)

