import {TrackerCapable} from "./tracker-events";

interface Tracker {
    newChannel: (channelId: string) => void
}

const NULL_TRACKER: Tracker = {
    newChannel() {
    }
};

export class GuildChannel extends TrackerCapable<Tracker> {
    static INSTANCE = new GuildChannel();

    private channels = new Map<string, string>();

    private constructor() {
        super(NULL_TRACKER);
    }

    getChannel(guildId: string): string | undefined {
        return this.channels.get(guildId);
    }

    setChannel(guildId: string, channelId: string) {
        const old = this.channels.get(guildId);
        console.log(guildId, channelId, 'ow', old);
        if (old !== channelId) {
            this.channels.set(guildId, channelId);

            this.eachTracker(guildId, t => t.newChannel(channelId));
        }
    }

}

export const GUILD_CHANNELS = GuildChannel.INSTANCE;