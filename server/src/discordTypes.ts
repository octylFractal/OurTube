export enum ChannelType {
    GUILD_TEXT,
    DM,
    GUILD_VOICE,
    GROUP_DM,
    GUILD_CATEGORY
}

export interface PartialGuildChannel {
    type: ChannelType
    name: string
    id: string
}

export interface PartialGuild {
    channels: { [key: string]: PartialGuildChannel },
    name: string
    id: string
}

// imported from discord.io types
export interface cbError {
    message?: string,
    statusCode?: string,
    statusMessage?: string,
    response?: string
}
