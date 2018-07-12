import React from "react";
import {selectGuild} from "../reduxish/store";
import {GuildKeyed, RawGuild} from "../reduxish/stateInterfaces";
import {GuildIcon} from "./GuildIcon";
import {Col, Row} from "reactstrap";
import {isNullOrUndefined} from "../preconditions";

export type DiscordGuildSelectProps = { guilds: GuildKeyed<RawGuild> };
export const DiscordGuildSelect: React.StatelessComponent<DiscordGuildSelectProps> = function DiscordGuildSelect(props) {
    const mappedGuilds = props.guilds.map((g, key) => {
        if (isNullOrUndefined(g) || isNullOrUndefined(key)) {
            return <div/>;
        }
        return <div key={key}>
            <GuildIcon idPrefix="guildSelector" guild={g} onClick={() => selectGuild(key)}/>
        </div>;
    }).toArray();
    return <Row>
        <Col md={5} className="m-auto">
            <span className="commutext">Select a guild to work with: </span>
            <div className="d-flex align-items-center justify-content-center flex-wrap">
                {mappedGuilds}
            </div>
        </Col>
    </Row>;
};