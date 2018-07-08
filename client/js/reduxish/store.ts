import {createStore, Store} from "redux";
import {ActionForSlice, afsFactory, buildActionMap} from "./slicing";
import {SongData} from "./SongData";
import {
    GuildId,
    GuildItem,
    InternalState,
    NicknameRecord,
    RawChannelArray,
    RawGuild,
    Song,
    SongDataRecord,
    SongQueue,
    UserId
} from "./stateInterfaces";
import {observeStoreSlice} from "./reduxObservers";
import {
    ChannelSelectedEvent,
    getApi,
    GuildCallbacks,
    initializeApi,
    SongProgressEvent,
    SongQueuedEvent,
    SongQueueEvent,
    SongVolumeEvent
} from "../websocket/api";
import {LSConst} from "../lsConst";
import {optional} from "../optional";
import {Immutable} from "../utils";
import {checkNotNull} from "../preconditions";
import {OTAction} from "./actionCreators";

const defaultState: InternalState = {
    availableChannels: Immutable.Map(),
    selectedChannelIds: Immutable.Map(),
    currentSongs: Immutable.Map(),
    songProgresses: Immutable.Map(),
    volumes: Immutable.Map(),
    songQueues: Immutable.Map(),
    guilds: Immutable.Map(),
    nicknameCaches: Immutable.Map(),
    songDataCaches: Immutable.Map(),
};

export async function getSongData(dataId: string): Promise<SongData> {
    const cache = OUR_STORE.getState().songDataCaches;
    let data = cache.get(dataId);
    if (data) {
        return data;
    }
    return (await getApi()).requestYoutubeSongData(dataId).then(data => {
        OUR_STORE.dispatch(Actions.cacheSongData({dataId: dataId, data: data}));
        return data;
    });
}

export async function getNickname(guildId: GuildId, userId: UserId): Promise<string> {
    const cache = OUR_STORE.getState().nicknameCaches;
    const cacheResult = optional(cache.get(guildId))
        .map(guildCache => guildCache.get(userId));
    if (cacheResult.isPresent()) {
        return cacheResult.value;
    }
    return (await getApi()).requestUserNickname(guildId, userId).then(data => {
        OUR_STORE.dispatch(Actions.cacheNickname({guildId: guildId, userId: userId, nickname: data}));
        return data;
    });
}

export function selectGuild(guildId: GuildId | undefined) {
    if (guildId) {
        localStorage.setItem(LSConst.DISCORD_GUILD_ID, guildId);
    } else {
        localStorage.removeItem(LSConst.DISCORD_GUILD_ID);
    }
    OUR_STORE.dispatch(Actions.selectGuild(guildId));
}

const slicer = afsFactory<InternalState>();

export const Actions = buildActionMap({
    setAccessToken: slicer.newAction('accessToken',
        (prevState, payload: string | undefined) => {
            return payload;
        }),
    selectGuild: slicer.newAction('visibleGuild',
        (prevState, payload: GuildId | undefined, fullPrevState) => {
            if (!fullPrevState.guilds.some((g, gId) => gId === payload)) {
                return prevState;
            }
            return payload;
        }),
    setChannels: slicer.newAction('availableChannels',
        (prevState, payload: GuildItem<RawChannelArray>) => {
            return prevState.set(payload.guildId, payload.item);
        }),
    selectChannel: slicer.newAction('selectedChannelIds',
        (prevState, payload: ChannelSelectedEvent) => {
            return prevState.set(payload.guildId, payload.channelId);
        }),
    setCurrentSong: slicer.newAction('currentSongs',
        (prevState, payload: SongQueueEvent) => {
            const startedSong = OUR_STORE.getState()
                .songQueues
                .get(payload.guildId, Immutable.Map())
                .get(payload.submitterId, Immutable.List())
                .filter(song => checkNotNull(song).queueId === payload.queueId)
                .first();
            if (!startedSong) {
                return prevState.set(payload.guildId, undefined);
            }
            // trigger a de-queue if it's found
            OUR_STORE.dispatch(Actions.skipSong(payload));
            return prevState.set(payload.guildId, startedSong);
        }),
    setProgress: slicer.newAction('songProgresses',
        (prevState, payload: SongProgressEvent) => {
            const currentSong = OUR_STORE.getState().currentSongs.get(payload.guildId);
            if (currentSong && currentSong.queueId === payload.queueId) {
                return prevState.set(payload.queueId, payload.progress);
            }
            return prevState;
        }),
    setVolume: slicer.newAction('volumes',
        (prevState, payload: SongVolumeEvent) => {
            return prevState.set(payload.guildId, payload.volume);
        }),
    queueSong: slicer.newAction('songQueues',
        (prevState: InternalState['songQueues'], payload: SongQueuedEvent) => {
            return prevState.update(payload.guildId, Immutable.Map<UserId, SongQueue>(), guildQueues => {
                return guildQueues.update(payload.submitterId, Immutable.List<Song>(), userQueue => {
                    return userQueue.push({
                        queueId: payload.queueId,
                        queueTime: new Date(payload.queueTime),
                        dataId: payload.dataId
                    });
                });
            });
        }),
    skipSong: slicer.newAction('songQueues',
        (prevState: InternalState['songQueues'], payload: SongQueueEvent) => {
            return prevState.update(payload.guildId, Immutable.Map<UserId, SongQueue>(), guildQueues => {
                return guildQueues.update(payload.submitterId, Immutable.List<Song>(), (userQueue: SongQueue) => {
                    return userQueue.filter(song => checkNotNull(song).queueId !== payload.queueId).toList();
                });
            });
        }),
    setGuilds: slicer.newAction('guilds',
        (prevState, payload: RawGuild[]) => {
            return Immutable.Seq(payload)
                .toKeyedSeq()
                .mapKeys((key, value) => checkNotNull(value).id)
                .toMap();
        }),
    cacheNickname: slicer.newAction('nicknameCaches',
        (prevState, payload: NicknameRecord) => {
            return prevState.update(payload.guildId, Immutable.Map(), guildCache => {
                return guildCache.set(payload.userId, payload.nickname);
            });
        }),
    cacheSongData: slicer.newAction('songDataCaches',
        (prevState, payload: SongDataRecord) => {
            return prevState.set(payload.dataId, payload.data);
        }),
});

const reducer = slicer.getReducer();

const reduxDevtools: (() => any) | undefined = (window as any)['__REDUX_DEVTOOLS_EXTENSION__'];

export const OUR_STORE: Store<InternalState, OTAction<any>> = createStore(reducer, defaultState, reduxDevtools && reduxDevtools());

function actionDispatcher<P>(actionCreator: ActionForSlice<any, P>): (payload: P) => void {
    return payload => OUR_STORE.dispatch(actionCreator(payload));
}

const GUILD_CALLBACKS: GuildCallbacks = {
    songQueued: actionDispatcher(Actions.queueSong),
    songSkipped: actionDispatcher(Actions.skipSong),
    songStarted: actionDispatcher(Actions.setCurrentSong),
    songProgressed: actionDispatcher(Actions.setProgress),
    volumeChanged: actionDispatcher(Actions.setVolume),
    channelSelected: actionDispatcher(Actions.selectChannel),
};

observeStoreSlice(OUR_STORE,
    state => state.accessToken,
    (accessToken: string | undefined) => {
        if (!accessToken) {
            return;
        }
        discordApiCall('/users/@me', accessToken, data => {
            initializeApi(accessToken, data.id);
            getApi().then(api => api.subscribeGuildEvents(GUILD_CALLBACKS));
        });
    }
);

export function discordApiCall(path: string, accessToken: string, callback: (data: any) => void) {
    $.get({
        url: 'https://discordapp.com/api/v6' + path,
        headers: {
            Authorization: 'Bearer ' + accessToken
        }
    }).fail((xhr, textStatus, errorThrown) => {
        if (xhr.status === 401) {
            // 401 Unauthorized, log the user out!
            OUR_STORE.dispatch(Actions.setAccessToken(undefined));
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

