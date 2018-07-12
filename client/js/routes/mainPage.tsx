import {React, Route} from "./rbase";
import DiscordLogin from "../components/DiscordLogin";
import {connect} from "react-redux";
import {GuildKeyed, InternalState, RawGuild, visibleEntry} from "../reduxish/stateInterfaces";
import {DiscordGuildSelect} from "../components/DiscordGuildSelect";
import SongQueueManager from "../components/SongQueueManager";

type MainPageProps = { loggedIn: boolean, guild?: RawGuild, guilds: GuildKeyed<RawGuild> }
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

const MainPage = connect((ourState: InternalState) => {
    return {
        loggedIn: !!ourState.accessToken,
        guild: visibleEntry(ourState.guilds, ourState).orElse(undefined),
        guilds: ourState.guilds
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
