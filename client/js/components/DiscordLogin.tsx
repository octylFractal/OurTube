import React from "react";
import {generateOAuthLink} from "../oauth";

export default () => {
    return <div>
        <span className="commutext">Please log in to Discord to use this application: </span>
        <a href={generateOAuthLink()}>
            <img className="d-inline-block hover-black rounded" height={64} src="img/Discord-Logo+Wordmark-Color.svg"/>
        </a>
    </div>;
};