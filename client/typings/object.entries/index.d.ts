declare const entries: EntriesShim;

declare module "object.entries" {
    export = entries;
}

interface EntriesShim {
    shim(): void;
}

declare interface Object {
    entries<K extends { [k in keyof K]: V }, V>(object: K): Array<[keyof K, V]>
}
