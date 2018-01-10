import secureRandom from "secure-random";
import {fromByteArray} from "base64-js";
import queryString from "query-string";
import {LSConst} from "./lsConst";

const baseUrl = 'https://discordapp.com/api/oauth2/authorize';
const clientId = '400219515310571520';
const redirectUri = window.location.origin + '/?discordRedirect=true';

export function generateOAuthLink(): string {
    const randomStateArray = secureRandom.randomUint8Array(16);
    const state = fromByteArray(randomStateArray);
    localStorage.setItem(LSConst.DISCORD_AUTH_STATE, state);
    return baseUrl + '?' + queryString.stringify({
        response_type: 'token',
        client_id: clientId,
        state: state,
        scope: 'guilds identify',
        redirect_uri: redirectUri
    });
}