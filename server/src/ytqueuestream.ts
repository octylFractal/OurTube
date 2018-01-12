import {SONG_QUEUE} from "./GuildQueue";
import {StreamDispatcher} from "discord.js";
import {SongData} from "./api";
import {getVideoData} from "./ytapi";
import {DISCORD_BOT} from "./discordBot";
import {SONG_PROGRESS} from "./SongProgress";
import * as Ffmpeg from "fluent-ffmpeg";
import {Readable, Duplex, Writable} from "stream";
import {EVENTS} from "./Event";
import ytdl = require("ytdl-core");
import {Unsubscribe} from "./tracker-events";
import {getTime as monoclockGetTime} from "monoclock";
import through2 = require('through2');
import multistream = require('multistream');
import {FactoryStream} from "multistream";

type StreamAcceptor = (stream: Readable) => StreamDispatcher;

function getTime() {
    const time = monoclockGetTime();
    let s = time.sec;
    if (time.nsec > (500 * 1000 * 1000)) {
        s += 1;
    }
    return s;
}

enum StreamState {
    ALIVE, FINISHED
}

function emitCurrentStream(stream: Readable, count: number, self: Readable): Promise<StreamState> {
    return new Promise<StreamState>(resolve => {
        let endState = StreamState.ALIVE;
        const closeListener = () => {
            endState = StreamState.FINISHED;
            stream.removeListener('close', closeListener);
            resolve(endState);
        };
        stream.addListener('close', closeListener);

        const listener = (data: Buffer) => {
            if (count == 1 || !self.push(data)) {
                stream.removeListener('data', listener);
                stream.removeListener('close', closeListener);
            }
        };
        stream.addListener('data', listener);
    });
}

type StreamData = {
    stream: Duplex,
    time: number,
    videoData: SongData
};

export class YtQueueStream {
    private guildId: string;
    private currentSub: Unsubscribe | undefined;
    private infiniStream: Duplex | undefined;
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

    start(streamAcceptor: StreamAcceptor) {
        const self = this;
        const factory: FactoryStream = (cb) => {
            console.log("Request for more stream data!");
            self.playNext().then(stream => {
                this.current = stream;
                const self = this;
                const t2 = stream.stream.pipe(through2(function (this, chunk, enc, cb) {
                    if (self.current) {
                        this.push(chunk);
                    } else {
                        this.push(null);
                    }
                    cb();
                }));
                t2.on('end', () => {
                    self.current = undefined;

                    console.log('End of song, trading over!');
                    SONG_QUEUE.popSong(self.guildId);
                });
                cb(null, t2);
            }, err => cb(err, null));
        };
        const stream = multistream(factory);
        this.infiniStream = through2();
        stream.pipe(this.infiniStream);

        const timer = DISCORD_BOT.setInterval(() => {
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
            })
        ];
        const disStream = streamAcceptor(this.infiniStream);
        disStream.on('debug', (msg: string) => console.log('debug', msg));
        disStream.on('error', (err: any) => {
            console.log('Error in readStream', err);
            subscriptions.forEach(unsub => unsub());
            DISCORD_BOT.clearInterval(timer);
        });
        disStream.on('end', () => {
            subscriptions.forEach(unsub => unsub());
            DISCORD_BOT.clearInterval(timer);
        });
    }

    cancel() {
        this.currentSub && this.currentSub();
    }
}

export function newQueueStream(guildId: string) {
    return new YtQueueStream(guildId);
}
