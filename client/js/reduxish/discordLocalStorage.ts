import {GuildKeyed, RawGuild} from "./stateInterfaces";
import {Actions, OUR_STORE} from "./store";
import {observeStoreSlice} from "./reduxObservers";
import {LSConst} from "../lsConst";


export function discordInformationFromLocalStorage() {
    const authToken = localStorage.getItem(LSConst.DISCORD_ACCESS_TOKEN);
    if (authToken) {
        OUR_STORE.dispatch(Actions.setAccessToken(authToken));
    }
    observeStoreSlice(OUR_STORE, state => state.guilds, (guilds: GuildKeyed<RawGuild>) => {
        const guild = localStorage.getItem(LSConst.DISCORD_GUILD_ID);
        if (guild && guilds && !OUR_STORE.getState().visibleGuild) {
            // select it if none selected and guild is present
            OUR_STORE.dispatch(Actions.selectGuild(guild));
        }
    });
}