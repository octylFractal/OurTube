export interface DiscordGuild {
    id: string
    name: string
    icon: string | null
}

export interface DiscordInformation {
    accessToken?: string
}

export interface GuildInformation {
    instance: DiscordGuild
    channels: DiscordChannel[]
}

export interface SelectedChannels {
    [guildId: string]: string | undefined
}

export interface DiscordChannel {
    id: string
    name: string
}

export interface GuildEvent<E> {
    guildId: string
    event: E
}