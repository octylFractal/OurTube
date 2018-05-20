import {DiscordGuild} from "./discord";
import {Actions, ISTATE} from "./store";
import {observeStoreSlice} from "./reduxObservers";
import {LSConst} from "../lsConst";


export function discordInformationFromLocalStorage() {
    const authToken = localStorage.getItem(LSConst.DISCORD_ACCESS_TOKEN);
    if (authToken) {
        ISTATE.dispatch(Actions.updateInformation({accessToken: authToken}));
    }
    observeStoreSlice(ISTATE, state => state.discordGuilds, (guilds: DiscordGuild[]) => {
        const guild = localStorage.getItem(LSConst.DISCORD_GUILD_ID);
        if (guild && guilds) {
            for (let g of guilds) {
                if (g.id === guild) {
                    ISTATE.dispatch(Actions.selectGuild(g));
                    return;
                }
            }
        }
    });
}