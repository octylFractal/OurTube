import {Unsubscribe} from "redux";

export type SubCallback<T> = (data: T) => void;

export interface Subscribable<T> {
    push(data: T): void

    subscribe(callback: SubCallback<T>): Unsubscribe
}


class SubImpl<T> implements Subscribable<T> {
    private subscriptions: Map<string, SubCallback<T>>;

    constructor() {
        this.subscriptions = new Map<string, SubCallback<T>>();
    }

    push(data: T) {
        this.subscriptions.forEach(v => v(data));
    }

    subscribe(callback: SubCallback<T>): Unsubscribe {
        const key = `${Math.random()}`;
        this.subscriptions.set(key, callback);
        return () => this.subscriptions.delete(key);
    }
}

export function subscribable<T>(): Subscribable<T> {
    return new SubImpl<T>();
}
