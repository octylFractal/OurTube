import React from "react";
import $ from "jquery";
import 'bootstrap';
import {Collapse, Navbar, NavbarBrand, NavbarToggler} from "reactstrap";
import {connect} from "react-redux";
import GuildIcon from "./components/GuildIcon";
import {DiscordGuild} from "./reduxish/discord";
import {InternalState, selectGuild} from "./reduxish/store";
import {optional} from "./optional";

class NavbarDatum {
    hashVal: string;

    constructor(public name: string, public endpoint: string) {
        this.hashVal = endpoint.substring(endpoint.indexOf('#') + 1);
    }
}

const navbarData = [
    new NavbarDatum("Home", "#"),
];

class PrimaryNavContainer extends React.Component<{}, { active: string }> {
    hashChangeHandler: () => void;

    constructor(props: any) {
        super(props);
        this.state = {
            active: location.hash.substring(1)
        };
        this.hashChangeHandler = () => {
            this.setState({
                active: location.hash.substring(1)
            });
        };
    }

    componentDidMount() {
        // immediately update, just in case
        this.hashChangeHandler();
        $(window).on('hashchange', this.hashChangeHandler);
    }

    componentWillUnmount() {
        $(window).off('hashchange', this.hashChangeHandler);
    }

    render() {
        return <ul className="navbar-nav mr-auto">
            <li className="nav-item">
                <div className="vertical-line-1 mr-3 visible-md"/>
            </li>
            {navbarData.map(data => {
                const isActive = this.state.active === data.hashVal;
                let liClass = "nav-item rounded px-2";
                if (isActive) {
                    liClass += " active bg-success";
                } else {
                    liClass += " bg-danger";
                }
                return <li className={liClass} key={data.endpoint}>
                    <a className="nav-link commutext" href={data.endpoint}>
                        {data.name}{isActive && <span className="sr-only"> (current)</span>}
                    </a>
                </li>;
            })}
        </ul>;
    }

}

function deleteThisNephew() {
    selectGuild(undefined);
}

const NavGuildIcon = (props: { guild: DiscordGuild }) => {
    return <GuildIcon idPrefix="navbar" guild={props.guild}
                      onClick={props.guild === UNKNOWN_GUILD ? undefined : deleteThisNephew}/>;
};
const UNKNOWN_GUILD: DiscordGuild = {
    id: 'fakeIdNumeroUno',
    name: '??? No Guild Selected',
    icon: null
};
const SelectedGuildIcon = connect((ISTATE: InternalState) => {
    return {
        guild: optional(ISTATE.guild).map(g => g.instance).orElse(UNKNOWN_GUILD)
    };
})(NavGuildIcon);

class OTNavbar extends React.Component<{}, { isOpen: boolean }> {
    toggle: () => void;

    constructor(props: {}) {
        super(props);

        this.toggle = () => {
            this.setState(prevState => {
                return {isOpen: !prevState.isOpen}
            });
        };
        this.state = {
            isOpen: false
        };
    }

    render(): React.ReactNode {
        const inviteLink = `
        https://discordapp.com/api/oauth2/authorize?client_id=400219515310571520&permissions=3146752&scope=bot
`.trim();
        return <Navbar dark expand="md">
            <NavbarBrand href="/">
                <img src="img/logo.png" width={64} className="d-inline-block mx-3" alt=""/>
                <strong className="commutext d-inline-block align-middle">OurTube</strong>
            </NavbarBrand>
            <NavbarToggler onClick={this.toggle}/>
            <Collapse isOpen={this.state.isOpen} navbar>
                <PrimaryNavContainer/>
                <ul className="navbar-nav ml-auto">
                    <li className="nav-item m-auto">
                        <a target="_blank" href={inviteLink}>
                            Invite me!
                        </a>
                    </li>
                    <li className="nav-item m-auto">
                        <SelectedGuildIcon/>
                    </li>
                </ul>
            </Collapse>
        </Navbar>;
    }
}

export function createNavbar() {
    return <OTNavbar/>;
}