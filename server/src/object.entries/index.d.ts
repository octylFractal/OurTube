declare module 'object.entries' {
    const module: {
        shim(): void
    };

    export = module;
}

declare interface Object {
    entries<V>(object: { [key: string]: V }): Array<[string, V]>
}