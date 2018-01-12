import {TrackerCapable} from "./tracker-events";

type Tracker = {
    skipSong(): void
};

const NULL_TRACKER: Tracker = {
    skipSong() {
    }
};

export class EventMap extends TrackerCapable<Tracker> {
    static INSTANCE = new EventMap();

    private constructor() {
        super(NULL_TRACKER);
    }

    skipSong(guildId: string) {
        this.eachTracker(guildId, t => t.skipSong());
    }

}

export const EVENTS = EventMap.INSTANCE;