import {SONG_QUEUE} from "./GuildQueue";
import * as google from "googleapis";
import {secrets} from "./secrets";
import {DISCORD_BOT, DISCORD_GUILDS} from "./discordBot";
import {Unsubscribe} from "./tracker-events";
import Socket = SocketIO.Socket;
import {GUILD_CHANNELS} from "./GuildChannel";
import {getVideoData, Thumbnail} from "./ytapi";
import {SONG_PROGRESS} from "./SongProgress";
import {EVENTS} from "./Event";
import {ChannelType} from "./discordTypes";

function emitQueued(ws: Socket, id: string) {
    ws.emit('songQueue.queued', {youtubeId: id});
}

function setupSongQueue(ws: Socket) {
    let subscriptions: Unsubscribe[] = [];
    ws.on('songQueue.subscribe', (guildId: string) => {
        // emit the entire queue for this guildId to the client
        SONG_QUEUE.getQueue(guildId).forEach(id => emitQueued(ws, id));

        subscriptions.push(
            SONG_QUEUE.on('pop', guildId, () => ws.emit('songQueue.popped')),
            SONG_QUEUE.on('push', guildId, (pushed: string) => emitQueued(ws, pushed)),

            SONG_PROGRESS.on('newProgress', guildId, progress => ws.emit('songQueue.progress', progress))
        );
    });
    ws.on('songQueue.unsubscribe', () => {
        subscriptions.forEach(unsub => unsub());
        subscriptions = [];
    });
    ws.on('songQueue.queue', (guildId: string, songId: string) => {
        console.log('Queue Song', guildId, songId);
        SONG_QUEUE.queueSong(guildId, songId);
    });
}

export interface SongData {
    id: string
    name: string
    thumbnail: Thumbnail
    // in millis
    duration: number
}

interface Response<T> {
    error: string | undefined
    value: T | undefined
}

class ErrorResponse implements Response<any> {
    error: string;
    value: undefined;

    constructor(error: string) {
        this.error = error;
        this.value = undefined;
    }
}

class GoodResponse<T> implements Response<T> {
    error: undefined;
    value: T;

    constructor(value: T) {
        this.error = undefined;
        this.value = value;
    }
}

type ResponseFunc<T> = (data: Response<T>) => void

function setupYoutubeQueries(ws: Socket) {
    ws.on('yt.songData', (songId: string, response: ResponseFunc<SongData>) => {
        getVideoData(songId)
            .then(value => response(new GoodResponse(value)))
            .catch(error => response(new ErrorResponse(error)));
    });
}

interface DiscordChannel {
    id: string
    name: string
}

function emitChannel(ws: Socket, id: string | undefined) {
    ws.emit('dis.selectedChannel', {channelId: id});
}

function setupDiscordQueries(ws: Socket) {
    // filters provided guilds by guilds the bot is in
    ws.on('dis.filterGuilds', (guildIds: string[], response: ResponseFunc<string[]>) => {
        const botGuilds = DISCORD_GUILDS;
        let filteredIds = guildIds.filter(gid => botGuilds[gid]);
        response(new GoodResponse(filteredIds));
    });
    ws.on('dis.channels', (guildId: string, response: ResponseFunc<DiscordChannel[]>) => {
        const guild = DISCORD_GUILDS[guildId];
        if (typeof guild === "undefined") {
            response(new ErrorResponse('bot is not part of that guild'));
            return;
        }
        const channels = Object.entries(guild.channels)
            .map(([k, ch]) => ch)
            .filter(ch => ch.type === ChannelType.GUILD_VOICE);
        const res = channels.map(ch => ({id: ch.id, name: ch.name}));
        response(new GoodResponse(res));
    });
    ws.on('dis.selectChannel', (guildId: string, channelId: string) => {
        GUILD_CHANNELS.setChannel(guildId, channelId);
    });

    let subscriptions: Unsubscribe[] = [];
    ws.on('dis.subscribe', (guildId: string) => {
        // emit the current channel
        emitChannel(ws, GUILD_CHANNELS.getChannel(guildId));

        subscriptions.push(
            GUILD_CHANNELS.on('newChannel', guildId, id => emitChannel(ws, id))
        );
    });
    ws.on('dis.unsubscribe', () => {
        subscriptions.forEach(unsub => unsub());
        subscriptions = [];
    });
}

function setupEvents(ws: Socket) {
    ws.on('event.skipSong', (guildId: string) => {
        EVENTS.skipSong(guildId);
    })
}

export function setupApi(websocket: Socket) {
    setupSongQueue(websocket);
    setupYoutubeQueries(websocket);
    setupDiscordQueries(websocket);
    setupEvents(websocket);
}