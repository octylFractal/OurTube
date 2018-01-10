import React from "react";
import {selectGuild} from "../reduxish/store";
import {DiscordGuild} from "../reduxish/discord";
import GuildIcon from "./GuildIcon";
import {Col, Row} from "reactstrap";

export default (props: { guilds: DiscordGuild[] }) => {
    const mappedGuilds = props.guilds.map(g => {
        return <div key={g.id}>
            <GuildIcon idPrefix="guildSelector" guild={g} onClick={() => selectGuild(g)}/>
        </div>;
    });
    return <Row>
        <Col md={5} className="m-auto">
            <span className="commutext">Select a guild to work with: </span>
            <div className="d-flex align-items-center justify-content-center flex-wrap">
                {mappedGuilds}
            </div>
        </Col>
    </Row>;
};