# PostmanPat

## Dependencies

This project has 3 dependencies for the features it provides
* Vault - [1.7]
* EssentialsX - [2.20.1]
* DiscordSRV - [1.28.1]

## Features

### Description

This plugin uses DiscordSRV's Bot instance and api to add SlashCommands to allow
for mail, and economy services.

### Commands

Mail Commands
* `/mail read [page] [include-read]`
* `/mail send <user> <message>`
* `/mail mark-read`
* `/mail ignore [user] [uuid]`

Econ Commands
* `/bal [user]`
* `/pay <user> <amount>`

Considerations have been made for negative values, zero values, insufficient balances,
and respects Essentials `/togglepay` feature.

### Config

The important config node is `NotifyChannel` which should be set
to a public textchannel for users who cannot receive a DM, to be pinged
about new mail. An invalid channel ID will log an error in console

Some sub-commands have been made configurable, as a bit of an oversight root commands
aren't currently configurable due to how commands have been implemented.

