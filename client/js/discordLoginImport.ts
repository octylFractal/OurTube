import {LSConst} from "./lsConst";

interface DiscordLoginData {
    access_token: string
    state: string

    // anything else is still ok
    [k: string]: any
}

export function discordLogin(hashData: DiscordLoginData) {
    const knownState = localStorage.getItem(LSConst.DISCORD_AUTH_STATE);
    if (hashData.state !== knownState) {
        return;
    }
    localStorage.setItem(LSConst.DISCORD_ACCESS_TOKEN, hashData.access_token);
}