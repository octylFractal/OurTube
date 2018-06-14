import React, {ReactNode} from 'react';
import {Tooltip, TooltipProps} from 'reactstrap';
import {isDefined} from "../preconditions";
import {Omit} from "type-zoo";
import {Unsubscribe} from "redux";
import {Subscribable} from "../sub";


interface PropAdditions extends TooltipProps {
    tooltipContent?: ReactNode,
    domNode: Subscribable<HTMLElement>
}

export type SimpleTooltipProps = Omit<PropAdditions, 'target'>;

type SimpleTooltipState = {
    tooltipOpen: boolean,
    ref: HTMLElement | null | undefined
};

export class SimpleTooltip extends React.Component<SimpleTooltipProps, SimpleTooltipState> {
    toggle: () => void;
    _unsub: Unsubscribe | undefined;

    constructor(props: SimpleTooltipProps) {
        super(props);

        this.toggle = () => {
            this.setState(prevState => ({
                ...prevState,
                tooltipOpen: !prevState.tooltipOpen
            }));
        };
        this.componentWillReceiveProps(props);
        this.state = {
            tooltipOpen: false,
            ref: undefined
        };
    }

    componentWillReceiveProps(props: SimpleTooltipProps) {
        if (this._unsub) {
            this._unsub();
        }
        this._unsub = props.domNode.subscribe(node => this.setState(prevState => {
            console.log('dom node acquired', node);
            return {...prevState, ref: node};
        }))
    }

    render() {
        const parts = [];
        if (isDefined(this.state.ref)) {
            console.log('defining tooltip!', this.state.ref);
            const ttProps = {...this.props};
            delete ttProps.tooltipContent;
            delete ttProps.domNode;
            parts.push(<Tooltip {...ttProps} target={this.state.ref} isOpen={this.state.tooltipOpen}
                                toggle={this.toggle}>
                {this.props.tooltipContent}
            </Tooltip>);
        }
        parts.push(this.props.children);
        return parts;
    }
}