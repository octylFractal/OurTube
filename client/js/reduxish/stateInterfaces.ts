/*
 * Some standards for this:
 * - RawX: represents data of X directly from Discord's API
 */

import {SongData} from "./SongData";
import {Immutable} from "../utils";
import {OUR_STORE} from "./store";
import {optional, Optional} from "../optional";

export type GuildId = string;
export type ChannelId = string;
/**
 * Globally unique ID for each song in the queue. Not the song's YT ID, because it's not unique.
 */
export type QueueId = string;
export type UserId = string;

export interface RawGuild {
    readonly id: GuildId
    readonly name: string
    readonly icon: string | null
}

export interface RawChannel {
    readonly id: ChannelId
    readonly name: string
}

export interface Song {
    readonly queueId: QueueId
    /**
     * Moment at which this song became queued. Used for sorting.
     */
    readonly queueTime: Date
    /**
     * ID for getting some SongData.
     */
    readonly dataId: string
}

export type SongQueue = Immutable.List<Song>;

export type RawChannelArray = Immutable.List<RawChannel>;

export type GuildKeyed<T> = Immutable.Map<GuildId, T>;

export function visibleEntry<T>(keyed: GuildKeyed<T>, state: InternalState): Optional<NonNullable<T>> {
    return optional(state.visibleGuild)
        .map(vg => keyed.get(vg));
}

export type UserSongQueues = Immutable.Map<UserId, SongQueue>;

export type GuildNickCache = Immutable.Map<UserId, string>;

export interface NicknameRecord {
    guildId: GuildId
    userId: UserId
    nickname: string
}

export interface SongDataRecord {
    dataId: string
    data: SongData
}

export interface InternalState {
    /**
     * Discord API token, if acquired.
     *
     * This token is a USER token.
     */
    accessToken?: string
    /**
     * Guild currently on-screen.
     */
    visibleGuild?: GuildId
    /**
     * Channels for each guild.
     */
    availableChannels: GuildKeyed<RawChannelArray | undefined>
    /**
     * Selected channel for each guild.
     */
    selectedChannelIds: GuildKeyed<ChannelId | undefined>
    /**
     * Users in our selected channel.
     */
    usersInChannels: GuildKeyed<Immutable.Set<UserId> | undefined>
    /**
     * Song playing in the guild, if any.
     */
    currentSongs: GuildKeyed<Song | undefined>
    /**
     * Progress reports for currently playing songs.
     */
    songProgresses: GuildKeyed<number>
    /**
     * Volume sliders for guilds.
     */
    volumes: GuildKeyed<number>
    /**
     * Queues for each user currently in voice chat.
     */
    songQueues: GuildKeyed<UserSongQueues | undefined>
    /**
     * Raw guilds that we know about.
     */
    guilds: GuildKeyed<RawGuild | undefined>
    /**
     * Nickname cache for guild members.
     */
    nicknameCaches: GuildKeyed<GuildNickCache | undefined>
    /**
     * Song data cache. Data is pulled via the server.
     */
    songDataCaches: Immutable.Map<string, SongData | undefined>
}