import {io} from "./wsbase";
import "socket.io-client";
import {SongData} from "../reduxish/SongData";
import {DiscordChannel} from "../reduxish/discord";

type Socket = SocketIOClient.Socket;

export type SongQueuedEvent = {
    youtubeId: string
}

export interface SongQueuedCallback {
    (event: SongQueuedEvent): void
}

export type SongPoppedEvent = {}

export interface SongPoppedCallback {
    (event: SongPoppedEvent): void
}

export type SongProgressEvent = {
    songId: string,
    progress: number
}

export interface SongProgressCallback {
    (event: SongProgressEvent): void
}

export type SongQueueCallbacks = {
    queued: SongQueuedCallback,
    popped: SongPoppedCallback,
    progress: SongProgressCallback
}


export type ChannelSelectedEvent = {
    channelId: string | undefined
}

export interface ChannelSelectedCallback {
    (event: ChannelSelectedEvent): void
}

export type DiscordCallbacks = {
    channelSelected: ChannelSelectedCallback
}


export interface Api {
    subscribeSongQueue(guildId: string, callbacks: SongQueueCallbacks): void

    unsubscribeSongQueue(guildId: string): void

    queueSong(guildId: string, songId: string): void

    requestYoutubeSongData(songId: string): Promise<SongData>

    filterGuildIds(guildIds: string[]): Promise<string[]>

    getChannels(guildId: string): Promise<DiscordChannel[]>

    subscribeDiscord(guildId: string, callbacks: DiscordCallbacks): void

    unsubscribeDiscord(guildId: string): void

    selectChannel(guildId: string, channelId: string): void

    skipSong(guildId: string): void

    close(): void
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

class ApiImpl implements Api {
    websocket: Socket;

    constructor(websocket: Socket) {
        this.websocket = websocket
    }

    subscribeSongQueue(guildId: string, callbacks: SongQueueCallbacks): void {
        this.websocket.on('songQueue.queued', callbacks.queued);
        this.websocket.on('songQueue.popped', callbacks.popped);
        this.websocket.on('songQueue.progress', callbacks.progress);
        this.websocket.emit('songQueue.subscribe', guildId);
    }

    unsubscribeSongQueue(): void {
        this.websocket.emit('songQueue.unsubscribe');
    }

    queueSong(guildId: string, songId: string): void {
        this.websocket.emit('songQueue.queue', guildId, songId);
    }

    private responseProtoToPromise<T>(event: string, ...args: any[]): Promise<T> {
        return new Promise<T>((resolve, reject) => {
            this.websocket.emit(event, ...args, (response: Response<T>) => {
                if (response.error) {
                    reject(response.error);
                } else {
                    resolve(response.value);
                }
            });
        });
    }

    requestYoutubeSongData(songId: string): Promise<SongData> {
        return this.responseProtoToPromise('yt.songData', songId);
    }

    filterGuildIds(guildIds: string[]): Promise<string[]> {
        return this.responseProtoToPromise('dis.filterGuilds', guildIds);
    }

    getChannels(guildId: string): Promise<DiscordChannel[]> {
        return this.responseProtoToPromise('dis.channels', guildId);
    }

    subscribeDiscord(guildId: string, callbacks: DiscordCallbacks): void {
        this.websocket.on('dis.selectedChannel', callbacks.channelSelected);
        this.websocket.emit('dis.subscribe', guildId);
    }

    unsubscribeDiscord(): void {
        this.websocket.emit('dis.unsubscribe');
    }

    selectChannel(guildId: string, channelId: string): void {
        this.websocket.emit('dis.selectChannel', guildId, channelId);
    }

    skipSong(guildId: string): void {
        this.websocket.emit('event.skipSong', guildId);
    }

    close(): void {
        this.websocket.close();
    }
}

export const API = new ApiImpl(io('/api', {
    path: '/server/transport-layer'
}));
