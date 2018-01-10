import {Actions, ISTATE} from "./reduxish/store";
import {NonNullable} from "type-zoo";

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

export interface ElvisOperator<T> {
    val: T | undefined

    <K extends keyof T>(key: K): ElvisOperator<NonNullable<T[K]>>
}

export interface DefinedElvisOperator<T> extends ElvisOperator<T> {
    val: T
}

export interface UndefinedElvisOperator extends ElvisOperator<any> {
    val: undefined
    (key: any): UndefinedElvisOperator
}

const undefinedElvis: UndefinedElvisOperator = Object.assign(() => undefinedElvis, {val: undefined});

export function elvis<T>(value: undefined): UndefinedElvisOperator;
export function elvis<T>(value: NonNullable<T>): DefinedElvisOperator<T>;
export function elvis<T>(value: NonNullable<T> | undefined): ElvisOperator<T> {
    if (typeof value === "undefined") {
        return undefinedElvis;
    }
    return Object.assign((k: keyof T) => elvis(value[k]), {val: value});
}

export const e = elvis;
