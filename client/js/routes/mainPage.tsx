import {React, Route} from "./rbase";
import DiscordLogin from "../components/DiscordLogin";
import {InternalState} from "../reduxish/store";
import {connect} from "react-redux";
import {isDefined} from "../preconditions";
import {DiscordGuild} from "../reduxish/discord";
import DiscordGuildSelect from "../components/DiscordGuildSelect";
import SongQueueManager from "../components/SongQueueManager";
import {e} from "../utils";

type MainPageProps = { loggedIn: boolean, guild?: DiscordGuild, guilds: DiscordGuild[] }
const MainPageTree = ({loggedIn, guild, guilds}: MainPageProps) => {
    if (!loggedIn) {
        return <DiscordLogin/>
    }
    if (!guild) {
        return <DiscordGuildSelect guilds={guilds}/>;
    }
    return <div>
        <SongQueueManager guildId={guild.id}/>
    </div>;
};

const MainPage = connect((ISTATE: InternalState) => {
    return {
        loggedIn: isDefined(ISTATE.discord),
        guild: e(ISTATE)('guild')('instance').val,
        guilds: ISTATE.discordGuilds
    }
})(MainPageTree);

export const mainPage: Route = {
    render: () => {
        return <div>
            <p className="commutext">WELCOME TO OURTUBE. THE PLACE TO SHARE YOUR SONGS FREELY.</p>
            <MainPage/>
        </div>;
    },
    title: 'Home'
};
