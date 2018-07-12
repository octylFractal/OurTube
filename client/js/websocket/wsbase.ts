import WebSocketClient from "@gamestdio/websocket";
import {noop} from "jquery";

export type RpcFunction = (args?: any) => void;

export class OurTubeRpc {
    private readonly websocket: WebSocketClient;
    private readonly wsCallbacks = new Map<string, Set<(e: any) => void>>();
    private readonly functions = new Map<string, RpcFunction>();
    private readonly callbackCounters = new Map<string, number>();
    private ready: boolean = false;

    constructor(url: string, readyCb: (() => void) = noop) {
        this.websocket = new WebSocketClient(url, undefined, {
            backoff: 'fibonacci'
        });
        this.websocket.listeners['onmessage'] = (e: any) => {
            const data = JSON.parse(e.data);
            const {name, arguments: args} = data;
            const fn = this.functions.get(name);
            if (!fn) {
                return;
            }
            fn(args);
        };
        this.addEventListener('reconnect', e => {
            console.info("WS Reconnected", e);
        });
        this.addEventListener('open', e => {
            this.ready = true;
            readyCb();
        });
        this.addEventListener('close', e => {
            this.ready = false;
        });
        this.websocket.listeners['onopen'] = (e: any) => this.callEventListeners('open', e);
        this.websocket.listeners['onclose'] = (e: any) => this.callEventListeners('close', e);
        this.websocket.listeners['onerror'] = (e: any) => this.callEventListeners('error', e);
        this.websocket.listeners['onreconnect'] = (e: any) => this.callEventListeners('reconnect', e);
    }

    private addEventListener(event: string, listener: (e: any) => void) {
        let eventCbs = this.wsCallbacks.get(event);
        if (!eventCbs) {
            this.wsCallbacks.set(event, eventCbs = new Set());
        }
        eventCbs.add(listener);
    }

    private removeEventListener(event: string, listener: (e: any) => void) {
        const eventCbs = this.wsCallbacks.get(event);
        if (!eventCbs) {
            return;
        }
        eventCbs.delete(listener);
    }

    private callEventListeners(event: string, e: any) {
        const eventCbs = this.wsCallbacks.get(event);
        if (!eventCbs) {
            return;
        }
        eventCbs.forEach(x => {
            x(e);
        });
    }

    readyPromise(): Promise<void> {
        if (this.ready) {
            return Promise.resolve();
        }
        return new Promise((resolve, reject) => {
            const openListener = () => {
                delListeners();
                resolve();
            };
            this.addEventListener('open', openListener);

            const errListener = (e: any) => {
                delListeners();
                reject(e);
            };
            this.addEventListener('error', errListener);

            const delListeners = () => {
                this.removeEventListener('open', openListener);
                this.removeEventListener('error', errListener);
            };
        });
    }

    register(event: string, func: RpcFunction) {
        this.functions.set(event, func);
    }

    remove(event: string) {
        this.functions.delete(event);
    }

    callFunction(name: string, args?: any) {
        this.websocket.send(JSON.stringify({
            name: name,
            // explicit null if undefined to ensure it's recorded as null
            arguments: args || null
        }));
    }

    createCallback(event: string, callback: RpcFunction): string {
        let cbNum = this.callbackCounters.get(event) || 0;
        cbNum++;
        this.callbackCounters.set(event, cbNum);
        const cbName = `${event}.callback${cbNum}`;
        const trace = new Error("Tracing error for leak tracking");

        // prevent memory leaks
        const removeTimer = setTimeout(() => {
            console.warn('Removing old callback for', event, 'it should have been called!', trace);
            this.remove(cbName);
        }, 60000);

        this.register(cbName, args => {
            try {
                callback(args);
            } finally {
                this.remove(cbName);
                clearTimeout(removeTimer);
            }
        });
        return cbName;
    }

    close() {
        this.websocket.close();
    }
}

