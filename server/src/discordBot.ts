import * as Discord from "discord.js";
import {secrets} from "./secrets";

function onJoinGuild() {

}

function setupBot() {
    const bot = new Discord.Client();
    bot.login(secrets.DISCORD_TOKEN).catch(err => console.error('Discord log in error!', err));

    bot.on('guildCreate', guild => {
        console.log('joins', guild.name);
    });
    bot.on('ready', () => {
        console.log('Ready as I will ever be.');
        console.log(bot.guilds);
    });

    return bot;
}

export const DISCORD_BOT = setupBot();
