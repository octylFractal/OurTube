import {SONG_QUEUE} from "./GuildQueue";
import {StreamDispatcher} from "discord.js";
import {SongData} from "./api";
import {getVideoData} from "./ytapi";
import {DISCORD_BOT} from "./discordBot";
import {SONG_PROGRESS} from "./SongProgress";
import * as Ffmpeg from "fluent-ffmpeg";
import {Readable} from "stream";
import {EVENTS} from "./Event";
import ytdl = require("ytdl-core");
import through = require("through");
import {Unsubscribe} from "./tracker-events";

type StreamAcceptor = (songData: SongData, stream: Readable) => StreamDispatcher;

export class YtQueueStream {
    private guildId: string;
    private currentSub: Unsubscribe | undefined;

    constructor(guildId: string) {
        this.guildId = guildId;
    }

    private play(song: string, streamAcceptor: StreamAcceptor) {
        const videoDataPromise = getVideoData(song);
        videoDataPromise.then(videoData => {
            return ytdl.getInfo('https://www.youtube.com/watch?v=' + song)
                .then(ytdlInfo => ({videoData: videoData, ytdlInfo: ytdlInfo}));
        }).then(({videoData, ytdlInfo}) => {
            let format = ytdl.chooseFormat(ytdlInfo.formats, {quality: 'lowest'});
            const audioOnlyFormats = ytdl.filterFormats(ytdlInfo.formats, 'audioonly')
                .sort((a, b) => a.audioBitrate - b.audioBitrate);
            if (audioOnlyFormats) {
                const opusFormat = audioOnlyFormats.filter(fmt => fmt.audioEncoding === 'opus');
                if (opusFormat) {
                    format = opusFormat[0];
                } else {
                    format = audioOnlyFormats[0];
                }
            }
            if (format instanceof Error) {
                throw format;
            }
            return {videoData: videoData, info: ytdlInfo, format: format};
        }).then(({videoData, info, format}) => {
            const stream = ytdl.downloadFromInfo(info, {
                format: format,
                highWaterMark: 10 * 1024 * 1024
            });
            const opus = Ffmpeg(stream, {
                logger: {
                    debug: data => console.log('FFDebug', data),
                    error: data => console.error('FFError', data),
                    info: data => console.log('FFInfo', data),
                    warning: data => console.log('FFWarn', data),
                }
            });
            opus
                .audioQuality(3)
                .outputFormat('mp3')
                .audioFilter('volume=0.3');
            const opusPipe = through();
            opus.pipe(opusPipe);

            const disStream = streamAcceptor(videoData, opusPipe);

            DISCORD_BOT.user.setGame(`${videoData.name}`)
                .catch(err => console.error('error setting game', err));

            const timer = DISCORD_BOT.setInterval(() => {
                const progress = (disStream.time / videoData.duration) * 100;

                SONG_PROGRESS.setProgress(this.guildId, {
                    songId: videoData.id,
                    progress: progress
                });
            }, 100);

            const subscriptions = [
                EVENTS.once('skipSong', this.guildId, () => {
                    disStream.end('skipped');
                })
            ];

            disStream.on('debug', msg => console.log('debug', msg));
            disStream.on('error', (err) => {
                console.log('Error in readStream', err);
            });
            disStream.on('end', () => {
                console.log('End of song, trading over!');
                DISCORD_BOT.clearInterval(timer);
                subscriptions.forEach(unsub => unsub());
                SONG_QUEUE.popSong(this.guildId);
                this.start(streamAcceptor);
            });
        }).catch(err => console.error('error with ffmpeg', err));
    }

    start(streamAcceptor: StreamAcceptor) {
        const popped = SONG_QUEUE.getQueue(this.guildId)[0];
        if (popped) {
            // we have a song ready
            this.play(popped, streamAcceptor);
        } else {
            DISCORD_BOT.user.setGame(`Nothing, queue something?`)
                .catch(err => console.error('error setting game', err));
            console.log('awaiting song');
            // await next song
            this.currentSub = SONG_QUEUE.once('push', this.guildId, () => {
                console.log('AWAIT finished, starting song');
                this.start(streamAcceptor);
            });
        }
    }

    cancel() {
        this.currentSub && this.currentSub();
    }
}

export function newQueueStream(guildId: string) {
    return new YtQueueStream(guildId);
}
