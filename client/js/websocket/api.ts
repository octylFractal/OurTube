import {OurTubeRpc} from "./wsbase";
import {SongData} from "../reduxish/SongData";
import {ChannelId, GuildId, QueueId, RawChannel, UserId} from "../reduxish/stateInterfaces";

export interface OurEvent {
    guildId: GuildId
}

export interface SongQueuedEvent extends OurEvent {
    dataId: string
    queueId: QueueId
    queueTime: number
    submitterId: UserId
}

export interface SongQueuedCallback {
    (event: SongQueuedEvent): void
}

export interface SongQueueEvent extends OurEvent {
    queueId: QueueId
    submitterId: UserId
}

export interface SongQueueEventCallback {
    (event: SongQueueEvent): void
}

export interface SongProgressEvent extends OurEvent {
    queueId: QueueId,
    progress: number
}

export interface SongProgressCallback {
    (event: SongProgressEvent): void
}

export interface SongVolumeEvent extends OurEvent {
    volume: number
}

export interface SongVolumeCallback {
    (event: SongVolumeEvent): void
}

export interface SongQueueCallbacks {
    queued: SongQueuedCallback
    popped: SongQueueEventCallback
    started: SongQueueEventCallback
    stopped: SongQueueEventCallback
    progress: SongProgressCallback
    volume: SongVolumeCallback
}

export interface ChannelSelectedEvent extends OurEvent {
    channelId: ChannelId | undefined
}

export interface ChannelSelectedCallback {
    (event: ChannelSelectedEvent): void
}

export interface DiscordCallbacks {
    channelSelected: ChannelSelectedCallback
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

class Api {
    private rpc: OurTubeRpc;

    constructor(websocket: OurTubeRpc) {
        this.rpc = websocket
    }

    subscribeSongQueue(guildId: GuildId, callbacks: SongQueueCallbacks): void {
        this.rpc.register('songQueue.queued', callbacks.queued);
        this.rpc.register('songQueue.popped', callbacks.popped);
        this.rpc.register('songQueue.progress', callbacks.progress);
        this.rpc.register('songQueue.volume', callbacks.volume);
        this.rpc.callFunction('songQueue.subscribe', guildId);
    }

    unsubscribeSongQueue(): void {
        this.rpc.remove('songQueue.queued');
        this.rpc.remove('songQueue.popped');
        this.rpc.remove('songQueue.progress');
        this.rpc.callFunction('songQueue.unsubscribe');
    }

    queueSongs(guildId: string, songUrl: string): void {
        this.rpc.callFunction('songQueue.queue', {
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

    requestYoutubeSongData(songId: string): Promise<SongData> {
        return this.responseProtoToPromise('yt.songData', cb => ({
            songId: songId,
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

    getMyGuilds(): Promise<GuildId[]> {
        return this.responseProtoToPromise('dis.myGuilds', cb => ({
            callbackName: cb
        }));
    }

    getChannels(guildId: string): Promise<RawChannel[]> {
        return this.responseProtoToPromise('dis.channels', cb => ({
            guildId: guildId,
            callbackName: cb
        }));
    }

    subscribeDiscord(guildId: string, callbacks: DiscordCallbacks): void {
        this.rpc.register('dis.selectedChannel', callbacks.channelSelected);
        this.rpc.callFunction('dis.subscribe', guildId);
    }

    unsubscribeDiscord(): void {
        this.rpc.remove('dis.selectedChannel');
        this.rpc.callFunction('dis.unsubscribe');
    }

    selectChannel(guildId: string, channelId: string | undefined | null): void {
        this.rpc.callFunction('dis.selectChannel', {
            guildId: guildId,
            channelId: channelId
        });
    }

    skipSong(guildId: string, songId: string): void {
        this.rpc.callFunction('event.skipSong', {
            guildId: guildId,
            songId: songId
        });
    }

    setVolume(guildId: string, volume: number): void {
        this.rpc.callFunction('songQueue.setVolume', {
            guildId: guildId,
            volume: volume
        });
    }

    close(): void {
        this.rpc.close();
    }
}

let API: Promise<Api> | undefined = undefined;

async function createApi(token: string, userId: string): Promise<Api> {
    const proto = window.location.protocol === 'https:' ? 'wss' : 'ws';
    const rpc = new OurTubeRpc(
        `${proto}://${window.location.host}/server/gateway?token=${encodeURIComponent(token)}&userId=${encodeURIComponent(userId)}`
    );
    await rpc.readyPromise();
    return new Api(rpc);
}

export function initializeApi(token: string, userId: string) {
    API = createApi(token, userId);
}

export function getApi(): Promise<Api> {
    if (typeof API === "undefined") {
        throw new Error("API not initialized");
    }
    return API.catch(err => {
        console.error("Tracing error for getApi", err);
        throw err;
    });
}

