package me.zodd.postmanpat.command

import github.scarsz.discordsrv.api.commands.PluginSlashCommand

/**
 * Provides a collection of `PluginSlashCommand` for DiscordSRV's
 * `SlashCommandProvider`
 */
interface PostmanCommandProvider {
    fun slashCommands() : Collection<PluginSlashCommand>
}