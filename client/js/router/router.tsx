import React from "react";
import $ from "jquery";

export interface Route {
    render(): JSX.Element

    cardWrap?: boolean
    title?: string
    displayTitle?: boolean
}

const NULL_ROUTE: Route = {
    render: () => <div>
        <p>404, not found!</p>
        <a href="#" className="btn btn-primary">Eh? You went <em>where?</em> You did <strong>what?!?!??</strong></a>
    </div>,
    title: "Negative, I am a meat popsicle"
};

type RouterProps = { paths: Map<string, Route> };

export class Router extends React.Component<RouterProps, { selected: string }> {
    hashEventListener: () => void;

    constructor(props: RouterProps) {
        super(props);
        this.state = {
            selected: location.hash.substring(1)
        };
        this.hashEventListener = () => {
            this.setState((prevState) => {
                const copy = {...prevState};
                copy.selected = location.hash.substring(1);
                return copy;
            });
        };
    }

    componentDidMount() {
        $(window).on('hashchange.router', this.hashEventListener);
    }

    componentWillUnmount() {
        $(window).off('hashchange.router', this.hashEventListener);
    }

    render() {
        let route = this.props.paths.get(this.state.selected) || NULL_ROUTE;
        let content = <div>
            {route.title && route.displayTitle !== false && <h3 className="card-title">{route.title}</h3>}
            {route.render()}
        </div>;
        return route.cardWrap !== false ? (
            <div className="card text-center">
                <div className="card-block">
                    <div className="m-3"/>
                    {content}
                    <div className="m-3"/>
                </div>
            </div>
        ) : content;
    }
}
