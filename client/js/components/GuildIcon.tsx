import React from "react";
import {RawGuild} from "../reduxish/stateInterfaces";
import {StatefulToolTip} from "react-portal-tooltip";

export default (props: { idPrefix: string, guild: RawGuild, onClick?: () => void }) => {
    const g = props.guild;
    const iconElement = (g.icon
        ? <img className="w-100 rounded-circle"
               src={`https://cdn.discordapp.com/icons/${g.id}/${g.icon}.png?size=512`}
               alt="Guild Icon"/>
        : <span className="commutext align-middle" style={{fontSize: '3em'}}>{g.name.charAt(0)}</span>);

    const tooltipParent = <div onClick={props.onClick}
                               className={props.onClick ? "cursor-pointer" : "cursor-default"}>
        {iconElement}
    </div>;
    return <div className="m-2 rounded-circle border-2 border-primary d-flex align-items-center justify-content-center"
                style={{width: 75, height: 75}}>
        <StatefulToolTip parent={tooltipParent} position="left" tooltipTimeout={0}>
            <span className="commutext">{g.name}</span>
        </StatefulToolTip>
    </div>;
};