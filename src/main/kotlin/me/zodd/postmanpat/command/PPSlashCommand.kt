package me.zodd.postmanpat.command

import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent

interface PPSlashCommand<T : PPSlashCommand<T>> {
    val command: String

    fun exec(): (SlashCommandEvent) -> Unit
}