import {createStore} from "redux";
import {annotateFunctions, createSliceDistributor, SliceMap} from "./slicing";
import {SongData} from "./SongData";
import {
    DiscordChannel, DiscordGuild, DiscordInformation, discordInformationFromLocalStorage,
    GuildInformation
} from "./discord";
import {observeStoreSlice} from "./reduxObservers";
import {discordApiCall, e} from "../utils";
import {API, Api} from "../websocket/api";
import {isDefined, isNullOrUndefined} from "../preconditions";
import {LSConst} from "../lsConst";

type SongDataCache = { [songKey: string]: SongData };

export interface SongProgress {
    songId: string
    progress: number
}

export interface InternalState {
    discord?: DiscordInformation
    guild?: GuildInformation
    discordGuilds: DiscordGuild[]
    songQueue: Array<string>
    songProgress?: SongProgress
    songDataCache: SongDataCache
}

const defaultState: InternalState = {
    discordGuilds: [],
    songQueue: [],
    songDataCache: {}
};

export function getSongData(songId: string): Promise<SongData> {
    const cache = ISTATE.getState().songDataCache;
    let data = cache[songId];
    if (data) {
        return Promise.resolve(data);
    }
    return API.requestYoutubeSongData(songId).then(data => {
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
    selectChannel: (prevState: GuildInformation, payload: string): GuildInformation => {
        if (isNullOrUndefined(prevState)) {
            throw new Error('no, this cannot be!');
        }
        return {...prevState, selectedChannel: payload};
    },
    setGuilds: (prevState, payload: DiscordGuild[]) => {
        return payload;
    },
    setChannels: (prevState: GuildInformation, payload: DiscordChannel[]): GuildInformation => {
        return {...prevState, channels: payload};
    },
    queueSong: (prevState: Array<string>, payload: string) => {
        return prevState.concat(payload);
    },
    popSong: (prevState: Array<string>) => {
        return prevState.slice(1);
    },
    updateProgress: (prevState: SongProgress, payload: SongProgress): SongProgress => {
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
    API.filterGuildIds(guildIds).then(filteredGuildIds => {
        const idSet = new Set(filteredGuildIds);
        const filtered = guilds.filter(g => idSet.has(g.id));
        ISTATE.dispatch(Actions.setGuilds(filtered));
    }).catch(err => console.error('Error filtering guilds', err));
}

observeStoreSlice(ISTATE, state => e(state)('discord')('accessToken').val, (accessToken: string | undefined) => {
    if (!accessToken) {
        return;
    }
    const cached = guildsCache.get(accessToken);
    if (cached) {
        setUnfilteredGuilds(cached);
        return;
    }
    discordApiCall('/users/@me/guilds', accessToken, data => {
        guildsCache.set(accessToken, data);
        setUnfilteredGuilds(data);
    });
});
// Initialize WS connection when guild selected
observeStoreSlice(ISTATE, state => e(state)('guild')('instance').val, (guild) => {
    if (!guild) {
        API.unsubscribeSongQueue();
        ISTATE.dispatch(Actions.setChannels([]));
        return;
    }
    API.getChannels(guild.id)
        .then(channels => ISTATE.dispatch(Actions.setChannels(channels)))
        .catch(err => console.error('error getting channels for', guild.id, err));
    API.subscribeSongQueue(guild.id, {
        queued(queueEvent) {
            getSongData(queueEvent.youtubeId)
                .catch(err => console.error('error getting data for', queueEvent, err));
            ISTATE.dispatch(Actions.queueSong(queueEvent.youtubeId));
        },
        popped() {
            ISTATE.dispatch(Actions.popSong(undefined));
        },
        progress(event) {
            ISTATE.dispatch(Actions.updateProgress(event));
        }
    });
    API.subscribeDiscord(guild.id, {
        channelSelected(event) {
            ISTATE.dispatch(Actions.selectChannel(event.channelId));
        }
    });
});

// Trigger all initialization, calling subscribed functions:
function initStore() {
    discordInformationFromLocalStorage();
}

initStore();

