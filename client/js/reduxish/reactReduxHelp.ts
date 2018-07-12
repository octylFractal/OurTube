/**
 * Stricter & easier type-checking for common mapper function flow.
 *
 * Types of props:
 * - EXPOSED_PROPS: the props that will be used on the component returned from the connect() call
 * - RESULT_PROPS: the props that the original component uses
 */
import {Omit} from "type-zoo";
import {ComponentType} from "react";
import {connect} from "react-redux";

export interface MapStateToProps<STATE, EXPOSED_PROPS, RESULT_PROPS> {
    /**
     * Takes the state from the store and the exposed props, and generates all props that were not originally exposed.
     */
    (state: STATE, ownProps: EXPOSED_PROPS): Omit<RESULT_PROPS, keyof EXPOSED_PROPS>
}

export function connectSimple<S, EXP, RES>(mapStateToProps: MapStateToProps<S, EXP, RES>, component: ComponentType<RES>) {
    return connect(mapStateToProps)(component);
}