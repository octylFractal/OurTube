export {default as Immutable} from "immutable";

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

export enum CompareResult {
    A_FIRST = -1,
    SAME = 0,
    B_FIRST = 1,
}

export function EXEC<T>(func: () => T): T {
    return func();
}

export function nextEventLoop<T>(func: () => T): Promise<T> {
    return new Promise(((resolve, reject) => {
        try {
            resolve(func())
        } catch (e) {
            reject(e);
        }
    }));
}

export function oKeys<T>(o: T): (keyof T)[] {
    return Object.keys(o) as (keyof T)[];
}

export function oStrKeys<T>(o: T): (StrKeys<T>)[] {
    return Object.keys(o).filter(k => typeof k === "string") as (StrKeys<T>)[];
}

export type PartialMap<K extends keyof any, T> = {
    [P in K]?: T
};
export type StrKeys<T> = Extract<keyof T, string>;
