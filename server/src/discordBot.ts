import {secrets} from "./secrets";
import {GUILD_CHANNELS} from "./GuildChannel";
import {newQueueStream, YtQueueStream} from "./ytqueuestream";
import {cbError, ChannelType, PartialGuild} from "./discordTypes";
import * as Discord from "discord.io";

function cbToPromise<T>(acceptor: (cb: (error: cbError, response: T) => void) => void): Promise<T> {
    return new Promise<T>((resolve, reject) => {
        acceptor((error, result) => {
            if (error) {
                reject(error);
            } else {
                resolve(result);
            }
        });
    });
}

function onJoinGuild(guild: PartialGuild) {
    console.log('joins', guild.name);
    DISCORD_GUILDS[guild.id] = guild;
    let currentQueueStream: YtQueueStream | undefined = undefined;
    GUILD_CHANNELS.on('newChannel', guild.id, channelId => {
        const channel = guild.channels[channelId];
        if (!channel) {
            console.log('Not a channel', channelId);
            return;
        }
        if (channel.type !== ChannelType.GUILD_VOICE) {
            console.log('Not a voice channel', channel);
            return;
        }
        currentQueueStream && currentQueueStream.cancel();
        cbToPromise(cb => DISCORD_BOT.joinVoiceChannel(channelId, cb))
            .then(() => {
                return cbToPromise<NodeJS.WritableStream>(cb => DISCORD_BOT.getAudioContext(channelId, cb));
            })
            .then(voiceStream => {
                currentQueueStream = newQueueStream(guild.id);
                currentQueueStream.start((stream) => {
                    stream.pipe(voiceStream);
                    return new Promise<void>(resolve => {
                        voiceStream.on('done', () => resolve());
                    });
                });
            })
            .catch(err => {
                console.error(`Error attaching to channel voice (id=${channelId}):`, err);
            });
    });
}

export const DISCORD_GUILDS: { [key: string]: PartialGuild } = {};

function setupBot() {
    const bot = new Discord.Client({
        token: secrets.DISCORD_TOKEN,
        autorun: true
    });
    bot.on('disconnect', (msg, code) => {
        console.error('Oof ouch owie my websocket', msg, code);
    });

    bot.on('guildCreate', (guild: PartialGuild) => {
        onJoinGuild(guild);
    });
    bot.on('ready', () => {
        console.log('Ready as I will ever be. Username=' + bot.username);
    });
    bot.on('message', (user, userID, channelID, message) => {
        if (bot.username !== user && bot.directMessages[channelID]) {
            const reply = message === '42' ? '**WHAT DO YOU GET WHEN YOU MULTIPLY SIX BY NINE?**' : 'no u';
            bot.sendMessage({
                to: channelID,
                message: reply
            });
        }
    });
    bot.on('error', (error: any) => {
        console.error('bot.error', error);
    });

    console.log('Bot initialized...');
    return bot;
}

export const DISCORD_BOT = setupBot();
