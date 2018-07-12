import {OurTubeRpc} from "./wsbase";
import {SongData} from "../reduxish/SongData";
import {ChannelId, GuildId, QueueId, RawChannel, RawGuild, UserId} from "../reduxish/stateInterfaces";
import {oStrKeys} from "../utils";

export interface OurEvent {
    guildId: GuildId
}

export interface SongQueuedEvent extends OurEvent {
    dataId: string
    queueId: QueueId
    queueTime: string
    submitterId: UserId
}

export interface SongQueueEvent extends OurEvent {
    queueId: QueueId
    submitterId: UserId
}

export interface SongProgressEvent extends OurEvent {
    queueId: QueueId,
    progress: number
}

export interface SongVolumeEvent extends OurEvent {
    volume: number
}

export interface ChannelSelectedEvent extends OurEvent {
    channelId: ChannelId | undefined
}

export interface AvailableChannelsEvent extends OurEvent {
    availableChannels: RawChannel[]
}

/**
 * User joins our currently selected channel.
 */
export interface UserJoinChannelEvent extends OurEvent {
    userId: UserId
}

/**
 * User leaves our currently joined channel.
 */
export interface UserLeaveChannelEvent extends OurEvent {
    userId: UserId
}

export interface GuildCallbacks {
    songQueued(event: SongQueuedEvent): void

    songSkipped(event: SongQueueEvent): void

    songStarted(event: SongQueueEvent): void

    songProgressed(event: SongProgressEvent): void

    volumeChanged(event: SongVolumeEvent): void

    availableChannels(event: AvailableChannelsEvent): void

    channelSelected(event: ChannelSelectedEvent): void

    userJoinChannel(event: UserJoinChannelEvent): void

    userLeaveChannel(event: UserLeaveChannelEvent): void
}

interface Response<T> {
    error: string | undefined
    value: T | undefined
}

interface ErrorResponse extends Response<any> {
    error: string
    value: undefined
}

interface GoodResponse<T> extends Response<T> {
    error: undefined
    value: T
}

const NULL_CALLBACKS: GuildCallbacks = {
    songQueued() {
    },
    songSkipped() {
    },
    songStarted() {
    },
    songProgressed() {
    },
    volumeChanged() {
    },
    availableChannels() {
    },
    channelSelected() {
    },
    userJoinChannel() {
    },
    userLeaveChannel() {
    },
};

class Api {
    readonly rpc: OurTubeRpc;

    constructor(websocket: OurTubeRpc) {
        this.rpc = websocket
    }

    subscribeGuildEvents(callbacks: GuildCallbacks): void {
        for (const key of oStrKeys(NULL_CALLBACKS)) {
            this.rpc.register(`guildEvents.${key}`, callbacks[key]);
        }
        this.rpc.callFunction('guildEvents.subscribe');
    }

    unsubscribeGuildEvents(): void {
        this.rpc.callFunction('guildEvents.unsubscribe');
        for (const key of oStrKeys(NULL_CALLBACKS)) {
            this.rpc.remove(`guildEvents.${key}`);
        }
    }

    queueSongs(guildId: string, songUrl: string): void {
        this.rpc.callFunction('guild.queue', {
            guildId: guildId,
            songUrl: songUrl
        });
    }

    private responseProtoToPromise<T>(event: string, argsMaker: (cbName: string) => any): Promise<T> {
        return new Promise<T>((resolve, reject) => {
            const cbName = this.rpc.createCallback(event, (response: Response<T>) => {
                if (response.error) {
                    reject(response.error);
                } else {
                    resolve(response.value);
                }
            });

            this.rpc.callFunction(event, argsMaker(cbName));
        });
    }

    requestYoutubeSongData(dataId: string): Promise<SongData> {
        return this.responseProtoToPromise('youtube.songData', cb => ({
            dataId: dataId,
            callbackName: cb
        }));
    }

    requestUserNickname(guildId: GuildId, userId: UserId): Promise<string> {
        return this.responseProtoToPromise('dis.userNickname', cb => ({
            guildId: guildId,
            userId: userId,
            callbackName: cb
        }));
    }

    getMyGuilds(): Promise<RawGuild[]> {
        return this.responseProtoToPromise('discord.myGuilds', cb => ({
            callbackName: cb
        }));
    }

    getChannels(guildId: string): Promise<RawChannel[]> {
        return this.responseProtoToPromise('guild.channels', cb => ({
            guildId: guildId,
            callbackName: cb
        }));
    }

    selectChannel(guildId: string, channelId: string | undefined | null): void {
        this.rpc.callFunction('guild.selectChannel', {
            guildId: guildId,
            channelId: channelId
        });
    }

    skipSong(guildId: string, queueId: string): void {
        this.rpc.callFunction('guild.skipSong', {
            guildId: guildId,
            queueId: queueId
        });
    }

    setVolume(guildId: string, volume: number): void {
        this.rpc.callFunction('guild.setVolume', {
            guildId: guildId,
            volume: volume
        });
    }

    close(): void {
        this.rpc.close();
    }
}

let API: Api | undefined = undefined;

function createApi(token: string, userId: string, readyCb: () => void): Api {
    const proto = window.location.protocol === 'https:' ? 'wss' : 'ws';
    const rpc = new OurTubeRpc(
        `${proto}://${window.location.host}/server/gateway?token=${encodeURIComponent(token)}&userId=${encodeURIComponent(userId)}`,
        readyCb
    );
    return new Api(rpc);
}

export function initializeApi(token: string, userId: string, readyCb: () => void) {
    API = createApi(token, userId, readyCb);
}

export async function getApi(): Promise<Api> {
    if (typeof API === "undefined") {
        throw new Error("API not initialized");
    }
    try {
        await API.rpc.readyPromise();
        return API;
    } catch (e) {
        console.error("Tracing error for getApi", e);
        throw e;
    }
}

