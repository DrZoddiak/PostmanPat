package me.zodd.postmanpat.config;

import me.zodd.postmanpat.PostmanPat
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.reference.ValueReference
import java.nio.file.Path
import kotlin.reflect.KClass


@ConfigSerializable
data class PostmanPatConfig(
    val notificationChannel: Long = 0L,
    val maxMessageSize: Int = 1900,
    val serverBranding: String = "Powered by PostmanPat",
    val commandConfig: CommandConfig = CommandConfig(),
) {
    companion object {
        val config = PostmanPatConfig()
    }
}

@ConfigSerializable
data class CommandConfig(
    val mailCommands: MailCommandConfig = MailCommandConfig(),
    val econCommands: EconCommandConfig = EconCommandConfig()
)

@ConfigSerializable
data class MailCommandConfig(
    val baseCommand: String = "mail",
    val readSubCommand: String = "read",
    val includeReadArg: String = "include-read",
    val sendSubCommand: String = "send",
    val markReadSubCommand: String = "mark-read",
    val ignoreSubCommand: String = "ignore",
)

@ConfigSerializable
data class EconCommandConfig(
    val payCommand: String = "pay",
    val balCommand: String = "balance",
    val sendSubCommand: String = "send",
    val markReadSubCommand: String = "mark-read",
    val ignoreSubCommand: String = "ignore",
    val minimumSendable: Double = 0.01,
    val currencySymbol: String = "$",
    val decimalFormat : String = "#,###.##",
)

class ConfigManager<T : Any>(plugin: PostmanPat, fileName: String, clazz: KClass<T>) {

    private val loader: HoconConfigurationLoader = HoconConfigurationLoader.builder()
        .path(Path.of("${plugin.dataPath}/$fileName.conf"))
        .defaultOptions { opts ->
            opts.serializers { serializer ->
                serializer.registerAnnotatedObjects(objectMapperFactory()).build()
            }
        }.build()

    private val root: ValueReference<T, CommentedConfigurationNode>? =
        loader.loadToReference().referenceTo(clazz.java)

    val conf: T = root?.get() ?: clazz.java.getDeclaredConstructor().newInstance()

    init {
        save()
    }

    fun save() {
        root?.setAndSave(conf)
    }
}