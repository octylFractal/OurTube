import {createStore} from "redux";
import {annotateFunctions, createSliceDistributor, SliceMap} from "./slicing";
import {SongData} from "./SongData";
import {DiscordChannel, DiscordGuild, DiscordInformation, GuildInformation} from "./discord";
import {observeStoreSlice} from "./reduxObservers";
import {getApi, initializeApi, SongQueuedEvent} from "../websocket/api";
import {LSConst} from "../lsConst";
import {optional} from "../optional";

type SongDataCache = { [songKey: string]: SongData };

export interface SongProgress {
    songId: string
    progress: number
}

export interface InternalState {
    discord?: DiscordInformation
    guild?: GuildInformation
    discordGuilds: DiscordGuild[]
    songQueue: Array<SongQueuedEvent>
    songProgress?: SongProgress
    volume?: number
    songDataCache: SongDataCache
}

const defaultState: InternalState = {
    discordGuilds: [],
    songQueue: [],
    songDataCache: {}
};

export async function getSongData(songId: string): Promise<SongData> {
    const cache = ISTATE.getState().songDataCache;
    let data = cache[songId];
    if (data) {
        return data;
    }
    return (await getApi()).requestYoutubeSongData(songId).then(data => {
        ISTATE.dispatch(Actions.cacheSongData({key: songId, value: data}));
        return data;
    });
}

export function selectGuild(guild?: DiscordGuild) {
    if (guild) {
        localStorage.setItem(LSConst.DISCORD_GUILD_ID, guild.id);
    } else {
        localStorage.removeItem(LSConst.DISCORD_GUILD_ID);
    }
    ISTATE.dispatch(Actions.selectGuild(guild));
}


export const Actions = annotateFunctions({
    updateInformation: (prevState, payload: DiscordInformation | undefined) => {
        return payload;
    },
    selectGuild: (prevState: GuildInformation | undefined, payload: DiscordGuild): GuildInformation => {
        return {channels: [], ...prevState, instance: payload};
    },
    selectChannel: (prevState: GuildInformation, payload: string | undefined): GuildInformation => {
        return {...prevState, selectedChannel: payload};
    },
    setGuilds: (prevState, payload: DiscordGuild[]) => {
        return payload;
    },
    setChannels: (prevState: GuildInformation, payload: DiscordChannel[]): GuildInformation => {
        return {...prevState, channels: payload};
    },
    queueSong: (prevState: Array<SongQueuedEvent>, payload: SongQueuedEvent) => {
        return prevState.concat(payload);
    },
    popSong: (prevState: Array<SongQueuedEvent>, payload: string) => {
        return prevState.filter(s => s.youtubeId !== payload);
    },
    updateProgress: (prevState: SongProgress, payload: SongProgress): SongProgress => {
        return payload;
    },
    setVolume: (prevState: number | undefined, payload: number | undefined): number | undefined => {
        return payload;
    },
    cacheSongData: (prevState: SongDataCache, payload: { key: string, value: SongData }) => {
        return {...prevState, [payload.key]: payload.value};
    }
});

const slices: SliceMap<InternalState> = {
    discord: [
        Actions.updateInformation
    ],
    guild: [
        Actions.selectGuild,
        Actions.selectChannel,
        Actions.setChannels
    ],
    discordGuilds: [
        Actions.setGuilds
    ],
    songQueue: [
        Actions.queueSong,
        Actions.popSong
    ],
    songProgress: [
        Actions.updateProgress
    ],
    volume: [
        Actions.setVolume
    ],
    songDataCache: [
        Actions.cacheSongData
    ]
};

const reducer = createSliceDistributor(slices, defaultState);

const reduxDevtools: (() => any) | undefined = (window as any)['__REDUX_DEVTOOLS_EXTENSION__'];

export const ISTATE = createStore(reducer, reduxDevtools && reduxDevtools());

const guildsCache = new Map<string, DiscordGuild[]>();

// Update Guilds when discord information becomes available:
function setUnfilteredGuilds(guilds: DiscordGuild[]) {
    const guildIds = guilds.map(g => g.id);
    getApi()
        .then(api => api.filterGuildIds(guildIds))
        .then(filteredGuildIds => {
            const idSet = new Set(filteredGuildIds);
            const filtered = guilds.filter(g => idSet.has(g.id));
            ISTATE.dispatch(Actions.setGuilds(filtered));
        })
        .catch(err => console.error('Error filtering guilds', err));
}

observeStoreSlice(ISTATE, state => optional(state).map(s => s.discord).map(d => d.accessToken).orElse(undefined), (accessToken: string | undefined) => {
    if (!accessToken) {
        return;
    }
    const cached = guildsCache.get(accessToken);
    if (cached) {
        setUnfilteredGuilds(cached);
        return;
    }
    discordApiCall('/users/@me', accessToken, data => {
        initializeApi(accessToken, data.id);
    });
    discordApiCall('/users/@me/guilds', accessToken, data => {
        guildsCache.set(accessToken, data);
        setUnfilteredGuilds(data);
    });
});
// Initialize WS connection when guild selected
observeStoreSlice(ISTATE, state => optional(state).map(s => s.guild).map(g => g.instance).orElse(undefined), (guild) => {
    getApi().then(api => {
        if (!guild) {
            api.unsubscribeSongQueue();
            api.unsubscribeDiscord();
            ISTATE.dispatch(Actions.setChannels([]));
            return;
        }
        api.getChannels(guild.id)
            .then(channels => ISTATE.dispatch(Actions.setChannels(channels)))
            .catch(err => console.error('error getting channels for', guild.id, err));
        api.subscribeSongQueue(guild.id, {
            queued(queueEvent) {
                getSongData(queueEvent.youtubeId)
                    .catch(err => console.error('error getting data for', queueEvent, err));
                ISTATE.dispatch(Actions.queueSong(queueEvent));
            },
            popped(event) {
                const song = event.songId;
                ISTATE.dispatch(Actions.popSong(song));
            },
            progress(event) {
                ISTATE.dispatch(Actions.updateProgress(event));
            },
            volume(event) {
                ISTATE.dispatch(Actions.setVolume(event.volume));
            }
        });
        api.subscribeDiscord(guild.id, {
            channelSelected(event) {
                ISTATE.dispatch(Actions.selectChannel(event.channelId));
            }
        });
    });
});

export function discordApiCall(path: string, accessToken: string, callback: (data: any) => void) {
    $.get({
        url: 'https://discordapp.com/api/v6' + path,
        headers: {
            Authorization: 'Bearer ' + accessToken
        }
    }).fail((xhr, textStatus, errorThrown) => {
        if (xhr.status === 401) {
            // 401 Unauthorized, log the user out!
            ISTATE.dispatch(Actions.updateInformation(undefined));
            return;
        }
        if (xhr.status == 429) {
            // rate limited!
            const retryMillis = xhr.responseJSON['retry_after'] || 10000;
            setTimeout(() => discordApiCall(path, accessToken, callback), retryMillis + 100);
            return;
        }
        console.log(`Failed to acquire ${path}:`, textStatus, errorThrown);
    }).done((data) => {
        callback(data);
    });
}

