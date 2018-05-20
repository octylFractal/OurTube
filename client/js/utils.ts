import {Actions, ISTATE} from "./reduxish/store";

export const OR_REDUCER = (previousValue: boolean, currentValue: any): boolean => {
    return previousValue || currentValue;
};
export const AND_REDUCER = (previousValue: boolean, currentValue: any): boolean => {
    return previousValue && currentValue;
};

export function anyFalse(iter: Iterable<any> | any[]) {
    const a: any[] = Array.isArray(iter) ? iter : Array.from(iter);
    return !a.reduce(AND_REDUCER, true);
}

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
