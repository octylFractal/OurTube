export type RpcFunction = (args?: any) => void;

export class OurTubeRpc {
    private readonly websocket: WebSocket;
    private readonly functions = new Map<string, RpcFunction>();
    private readonly callbackCounters = new Map<string, number>();

    constructor(url: string) {
        this.websocket = new WebSocket(url);
        this.websocket.addEventListener('message', e => {
            const data = JSON.parse(e.data);
            const {name, arguments: args} = data;
            const fn = this.functions.get(name);
            if (!fn) {
                return;
            }
            fn(args);
        });
    }

    readyPromise() {
        return new Promise((resolve, reject) => {
            const openListener = () => {
                this.websocket.removeEventListener('open', openListener);
                resolve();
            };
            this.websocket.addEventListener('open', openListener);
            const errListener = () => {
                this.websocket.removeEventListener('error', errListener);
                reject();
            };
            this.websocket.addEventListener('error', errListener);
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
            arguments: args
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

