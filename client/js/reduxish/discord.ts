export interface DiscordGuild {
    id: string
    name: string
    icon: string | null
}

export interface DiscordInformation {
    accessToken: string
}

export interface GuildInformation {
    instance: DiscordGuild
    channels: DiscordChannel[]
    selectedChannel?: string
}

export interface DiscordChannel {
    id: string
    name: string
}