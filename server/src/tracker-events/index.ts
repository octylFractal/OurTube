export interface Unsubscribe {
    (): void
}

export class TrackerCapable<TRACKER extends {[k: string]: Function}> {
    private nullTracker: TRACKER;
    private trackers = new Map<string, Map<string, TRACKER>>();

    constructor(nullTracker: TRACKER) {
        this.nullTracker = nullTracker;
    }

    private getTrackerMapOrCreate(guildId: string): Map<string, TRACKER> {
        let trMap = this.trackers.get(guildId);
        if (typeof trMap === "undefined") {
            this.trackers.set(guildId, trMap = new Map<string, TRACKER>());
        }
        return trMap;
    }

    eachTracker(guildId: string, each: (tracker: TRACKER) => void) {
        let trMap = this.trackers.get(guildId);
        if (typeof trMap !== "undefined") {
            trMap.forEach(each);
        }
    }

    on<K extends keyof TRACKER>(event: K, guildId: string, tracker: TRACKER[K]): Unsubscribe {
        const key = `${Math.random()}`;
        const t = Object.assign(
            {},
            this.nullTracker,
            {[event]: tracker}
        );
        const map = this.getTrackerMapOrCreate(guildId);
        map.set(key, t);
        return () => map.delete(key);
    }

    once<K extends keyof TRACKER>(event: K, guildId: string, tracker: TRACKER[K]): Unsubscribe {
        const unsub = this.on(event, guildId, () => {
            unsub();
            tracker.call(this, arguments);
        });
        return unsub;
    }
}