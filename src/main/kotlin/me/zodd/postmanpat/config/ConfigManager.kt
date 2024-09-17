package me.zodd.postmanpat.config

import me.zodd.postmanpat.PostmanPat
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.reference.ValueReference
import org.spongepowered.configurate.serialize.SerializationException
import java.nio.file.Path
import kotlin.reflect.KClass

class ConfigManager<T : Any>(private val plugin: PostmanPat, fileName: String, clazz: KClass<T>) {
    private val loader: HoconConfigurationLoader = HoconConfigurationLoader.builder()
        .path(Path.of("${plugin.dataPath}/$fileName.conf"))
        .defaultOptions { opts ->
            opts.serializers { serializer ->
                serializer.registerAnnotatedObjects(objectMapperFactory()).build()
            }
        }.build()

    private val root: ValueReference<T, CommentedConfigurationNode>? =
        loader.loadToReference().referenceTo(clazz.java)

    val conf: T = root?.get() ?: run {
        plugin.server.pluginManager.disablePlugin(plugin)
        throw SerializationException("Failed to load configuration file! Disabling plugin.")
    }

    init {
        plugin.logger.info("Loading config: $fileName")
        save()
    }

    fun save() {
        conf.runCatching {
            root?.setAndSave(this)
        }.onFailure {
            plugin.logger.warning("Failed to save configuration file.")
        }

    }
}
