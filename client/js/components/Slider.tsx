import React from "react";
import {optional} from "../optional";

export interface SliderProps {
    value: number,
    min?: number,
    max?: number,
    onValueSet?: (value: number) => any
}

type SliderState = {
    dragValue: number | undefined
};

export class Slider extends React.Component<SliderProps, SliderState> {
    boundMoveHandler: () => any;
    boundUpHandler: () => any;
    bar: HTMLElement | null;

    constructor(props: SliderProps) {
        super(props);
        this.boundMoveHandler = this.handleMouseMove.bind(this);
        this.boundUpHandler = this.handleMouseUp.bind(this);
        this.bar = null;

        this.state = {
            dragValue: undefined
        };
    }

    get min() {
        return optional(this.props.min).orElse(0);
    }

    get max() {
        return optional(this.props.max).orElse(100);
    }

    get value() {
        const dv = this.state.dragValue;
        return typeof dv !== "undefined" ? dv : this.props.value;
    }

    get dragging() {
        return typeof this.state.dragValue !== "undefined";
    }

    private handleMouseMove(e: MouseEvent) {
        if (this.bar === null) {
            return;
        }
        const rect = this.bar.getBoundingClientRect();
        const baseX = rect.left;
        const mx = e.pageX;
        let dx = mx - baseX;
        if (dx < 0) {
            dx = 0;
        } else if (dx > rect.width) {
            dx = rect.width;
        }
        const percent = dx / rect.width;
        const value = Math.round(this.min + (this.max - this.min) * percent);

        this.setState(state => ({...state, dragValue: value}));
    }

    private handleMouseUp() {
        this.updateDragging(false);
    }

    private updateDragging(dragging: boolean) {
        let dragValue: number | undefined;
        if (dragging) {
            document.addEventListener('mousemove', this.boundMoveHandler);
            document.addEventListener('mouseup', this.boundUpHandler);
            dragValue = this.value;
        } else {
            document.removeEventListener('mousemove', this.boundMoveHandler);
            document.removeEventListener('mouseup', this.boundUpHandler);

            const set = this.props.onValueSet;
            set && set(this.value);
            dragValue = undefined;
        }
        this.setState(state => ({...state, dragValue: dragValue}));
    }

    render() {
        const {min, max} = this;
        const value = this.value;
        let fillRatio = 100 * (value - min) / (max - min);
        if (value === min) {
            fillRatio = 0;
        }

        return <div className="d-flex w-100 h-100">
            {this.renderBar(fillRatio)}
            <span className="ml-3 text-right" style={{
                width: '3ex'
            }}>
                {value.toFixed(0)}
            </span>
        </div>;
    }

    private renderBar(fillRatio: number) {
        return <div
            className="d-flex justify-content-start align-content-stretch h-100 border rounded border-dark bg-secondary"
            style={{
                flexGrow: 1
            }}
            ref={ref => this.bar = ref}>
            <div className="bg-dark" style={{
                width: `${fillRatio}%`
            }}/>
            <div style={{width: 0}}>
                <div className="position-absolute btn-primary border rounded border-dark h-75"
                     style={{
                         width: '1vw',
                         top: `12.5%`,
                         transform: 'translate(-0.5vw, 0)',
                         cursor: 'pointer'
                     }}
                     onMouseDown={() => this.updateDragging(true)}
                >
                </div>
            </div>
        </div>;
    }
}
