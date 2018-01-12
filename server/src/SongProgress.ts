import {TrackerCapable} from "./tracker-events";

export interface SongProgress {
    songId: string,
    progress: number
}

type Tracker = {
    newProgress: (progress: SongProgress) => void
};

const NULL_TRACKER: Tracker = {
    newProgress() {
    }
};

export class SongProgressMap extends TrackerCapable<Tracker> {
    static INSTANCE = new SongProgressMap();

    private progressMap = new Map<string, SongProgress>();

    private constructor() {
        super(NULL_TRACKER);
    }

    getProgress(guildId: string): SongProgress | undefined {
        return this.progressMap.get(guildId);
    }

    setProgress(guildId: string, progress: SongProgress) {
        const old = this.progressMap.get(guildId);
        if (old !== progress) {
            this.progressMap.set(guildId, progress);

            this.eachTracker(guildId, t => t.newProgress(progress));
        }
    }

}

export const SONG_PROGRESS = SongProgressMap.INSTANCE;