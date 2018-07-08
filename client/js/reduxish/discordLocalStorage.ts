import {GuildKeyed, RawGuild} from "./stateInterfaces";
import {Actions, ISTATE} from "./store";
import {observeStoreSlice} from "./reduxObservers";
import {LSConst} from "../lsConst";


export function discordInformationFromLocalStorage() {
    const authToken = localStorage.getItem(LSConst.DISCORD_ACCESS_TOKEN);
    if (authToken) {
        ISTATE.dispatch(Actions.setAccessToken(authToken));
    }
    observeStoreSlice(ISTATE, state => state.guilds, (guilds: GuildKeyed<RawGuild>) => {
        const guild = localStorage.getItem(LSConst.DISCORD_GUILD_ID);
        if (guild && guilds && !ISTATE.getState().visibleGuild) {
            // select it if none selected and guild is present
            ISTATE.dispatch(Actions.selectGuild(guild));
        }
    });
}