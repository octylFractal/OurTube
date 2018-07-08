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
