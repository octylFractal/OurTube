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
