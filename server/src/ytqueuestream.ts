import {SONG_QUEUE} from "./GuildQueue";
import {SongData} from "./api";
import {getVideoData} from "./ytapi";
import {SONG_PROGRESS} from "./SongProgress";
import * as Ffmpeg from "fluent-ffmpeg";
import {Duplex, Readable, Transform} from "stream";
import {EVENTS} from "./Event";
import {Unsubscribe} from "./tracker-events";
import {getTime as monoclockGetTime} from "monoclock";
import ytdl = require("ytdl-core");
import through2 = require('through2');

// promise is the done signal, when it completes, the stream is done playing.
type StreamAcceptor = (stream: Readable) => Promise<void>;

function getTime() {
    const time = monoclockGetTime();
    let s = time.sec;
    if (time.nsec > (500 * 1000 * 1000)) {
        s += 1;
    }
    return s;
}


type StreamData = {
    stream: Duplex,
    time: number,
    videoData: SongData
};

class BestTransform extends Transform {
    canceled: boolean;
    constructor() {
        super({
            allowHalfOpen: false
        });
        this.canceled = false;
    }
    _transform(chunk: any, encoding: string, callback: Function) {
        if (!this.canceled) {
            console.log('Pushing...');
            this.push(chunk);
        } else {
            console.log('Pushing EOS');
            this.push(null);
            this.destroy();
        }
        callback();
    }
}

export class YtQueueStream {
    private guildId: string;
    private currentSub: Unsubscribe | undefined;
    private current: StreamData | undefined;

    constructor(guildId: string) {
        this.guildId = guildId;
    }

    private play(song: string): Promise<StreamData> {
        const videoDataPromise = getVideoData(song);
        return videoDataPromise.then(videoData => {
            return ytdl.getInfo('https://www.youtube.com/watch?v=' + song)
                .then(ytdlInfo => ({videoData: videoData, ytdlInfo: ytdlInfo}));
        }).then(({videoData, ytdlInfo}) => {
            const formats = ytdlInfo.formats;
            let highest = formats[0];
            for (let format of formats) {
                if (!highest.audioBitrate || (format.audioBitrate
                        && (format.audioBitrate > highest.audioBitrate
                            || (format.audioBitrate === highest.audioBitrate && !format.encoding))))
                    highest = format;
            }
            console.log("Fulfilling YTDL with", highest.audioEncoding + "/" + highest.audioBitrate);
            return {videoData: videoData, info: ytdlInfo, format: highest};
        }).then(({videoData, info, format}) => {
            const stream = ytdl.downloadFromInfo(info, {
                format: format
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
                .audioChannels(2)
                .outputFormat('wav')
                .audioFilter('volume=0.3');
            const outputStream = through2();
            opus.pipe(outputStream);

            return {
                stream: outputStream,
                videoData: videoData,
                time: getTime()
            };
        });
    }

    playNext(): Promise<StreamData> {
        return new Promise<StreamData>(resolve => {
            const popped = SONG_QUEUE.getQueue(this.guildId)[0];
            if (popped) {
                // we have a song ready
                resolve(this.play(popped));
            } else {
                console.log('awaiting song');
                // await next song
                this.currentSub = SONG_QUEUE.once('push', this.guildId, () => {
                    console.log('AWAIT finished, starting song');
                    resolve(this.playNext());
                });
            }
        });
    }

    playAgain(streamAcceptor: StreamAcceptor) {
        this.playNext().then(stream => {
            this.current = stream;
            const self = this;
            const t2 = new BestTransform();
            stream.stream.pipe(t2);

            const timer = setInterval(() => {
                if (this.current === undefined) {
                    return;
                }
                const newTime = getTime();
                const diffTime = newTime - this.current.time;
                const diffTimeMs = diffTime * 1000;
                const progress = (diffTimeMs / this.current.videoData.duration) * 100;

                SONG_PROGRESS.setProgress(this.guildId, {
                    songId: this.current.videoData.id,
                    progress: progress
                });
            }, 100);

            const subscriptions = [
                EVENTS.on('skipSong', this.guildId, () => {
                    this.current = undefined;
                    t2.canceled = true;
                    stream.stream.unpipe(t2);
                    stream.stream.destroy();
                })
            ];

            streamAcceptor(t2)
                .then(() => {
                    self.current = undefined;

                    clearInterval(timer);

                    subscriptions.forEach(usub => usub());

                    console.log('End of song, trading over!');
                    SONG_QUEUE.popSong(self.guildId);
                    setTimeout(() => this.playAgain(streamAcceptor), 100);
                });
        }, err => console.error('stream error', err));
    }

    start(streamAcceptor: StreamAcceptor) {
        this.playAgain(streamAcceptor);
    }

    cancel() {
        this.currentSub && this.currentSub();
    }
}

export function newQueueStream(guildId: string) {
    return new YtQueueStream(guildId);
}
