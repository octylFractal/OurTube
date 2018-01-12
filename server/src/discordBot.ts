import * as Discord from "discord.js";
import {secrets} from "./secrets";
import {GUILD_CHANNELS} from "./GuildChannel";
import {newQueueStream, YtQueueStream} from "./ytqueuestream";

function onJoinGuild(guild: Discord.Guild) {
    console.log('joins', guild.name);
    let currentQueueStream: YtQueueStream | undefined = undefined;
    GUILD_CHANNELS.on('newChannel', guild.id, channelId => {
        const channel = guild.channels.get(channelId);
        if (!channel) {
            return;
        }
        if (!(channel instanceof Discord.VoiceChannel)) {
            return;
        }
        let vc = guild.voiceConnection;
        vc && vc.disconnect();
        currentQueueStream && currentQueueStream.cancel();
        channel.join()
            .then(voiceConnection => {
                voiceConnection.on('debug', msg => {
                    console.log('VC debug', msg);
                });
                voiceConnection.on('warn', msg => {
                    console.log('VC warn', msg);
                });
                voiceConnection.on('disconnect', msg => console.log('VC disconnected.', msg));
                voiceConnection.on('reconnecting', () => console.log('VC reconnecting.'));
                currentQueueStream = newQueueStream(guild.id);
                currentQueueStream.start((stream) => {
                    return voiceConnection.playStream(stream);
                });
            })
            .catch(err => {
                console.error('Failed to join voice channel', channelId, err);
            });
    });
}

function setupBot() {
    const bot = new Discord.Client();
    bot.login(secrets.DISCORD_TOKEN).catch(err => console.error('Discord log in error!', err));

    bot.on('guildCreate', guild => {
        onJoinGuild(guild);
    });
    bot.on('ready', () => {
        console.log('Ready as I will ever be.');
        bot.guilds.forEach(onJoinGuild);
    });
    bot.on('message', (e) => {
        if (e.channel.type === 'dm') {
            e.author.sendMessage('no u').catch(err => console.log('error', err, e.author));
        }
    });
    bot.on('error', error => {
        console.error('bot.error', error);
    });

    return bot;
}

export const DISCORD_BOT = setupBot();
