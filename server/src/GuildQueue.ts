import {TrackerCapable} from "./tracker-events";

interface Tracker {
    pop: () => void
    push: (songId: string) => void
}

const NULL_TRACKER: Tracker = {
    pop() {
    },
    push() {
    }
};

export class GuildQueue extends TrackerCapable<Tracker> {
    static INSTANCE = new GuildQueue();

    private songQueues = new Map<string, string[]>();

    private constructor() {
        super(NULL_TRACKER);
    }

    getQueue(guildId: string): string[] {
        return this.songQueues.get(guildId) || [];
    }

    getQueueCreateIfNeeded(guildId: string): string[] {
        let queue = this.songQueues.get(guildId);
        if (typeof queue === 'undefined') {
            this.songQueues.set(guildId, queue = []);
        }
        return queue;
    }

    queueSong(guildId: string, songId: string) {
        this.getQueueCreateIfNeeded(guildId).push(songId);
        this.eachTracker(guildId, t => t.push(songId));
    }

    popSong(guildId: string): string | undefined {
        const popped = this.getQueue(guildId).shift();
        this.eachTracker(guildId, t => t.pop());
        return popped;
    }

}

export const SONG_QUEUE = GuildQueue.INSTANCE;