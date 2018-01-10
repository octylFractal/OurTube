import React from 'react';
import {Tooltip, TooltipProps} from 'reactstrap';

export type SimpleTooltipProps = TooltipProps;

export class SimpleTooltip extends React.Component<SimpleTooltipProps, { tooltipOpen: boolean }> {
    toggle: () => void;

    constructor(props: SimpleTooltipProps) {
        super(props);

        this.toggle = () => {
            this.setState(prevState => ({
                ...prevState,
                tooltipOpen: !prevState.tooltipOpen
            }));
        };
        this.state = {
            tooltipOpen: false
        };
    }

    render() {
        return (
            <Tooltip {...this.props} isOpen={this.state.tooltipOpen} toggle={this.toggle}>
                {this.props.children}
            </Tooltip>
        );
    }
}