import {Reducer} from "redux";
import {isDefined} from "../preconditions";
import {newAction, OTAction} from "./actionCreators";

export interface StateTransform<T, P> {
    (previousState: T | undefined, payload: P): T
}

export interface SliceFunction<T, P> {
    (payload: P): OTAction<P>

    type: string
    stateTransform: StateTransform<T, P>
}

export function newSliceFunction<T, P>(type: string, func: StateTransform<T, P>): SliceFunction<T, P> {
    return Object.assign((p: P) => newAction(type, p), {
        type: type,
        stateTransform: func
    });
}

type StringIndexable<V> = { [key: string]: V };
type Annotated<K> = { [k in keyof K]: SliceFunction<any, any> };

export function annotateFunctions<K extends StringIndexable<StateTransform<any, any>>>(funcs: K): Annotated<K> {
    const o = {} as Annotated<K>;
    Object.entries(funcs).forEach(([k, v]) => {
        o[k] = newSliceFunction(k, v);
    });
    return o;
}


export type SliceMap<T> = {[K in keyof T]: SliceFunction<T[K], any>[]};

interface Slice<T> {
    func: StateTransform<T, any>
    slice: string
}


export function createSliceDistributor<STATE extends StringIndexable<any>>(sliceMap: SliceMap<STATE>, defaultState: STATE): Reducer<STATE, OTAction<any>> {
    // rewrite sliceMap
    // map of actionType -> Slice
    const actionMap: Map<string, Slice<any>> = new Map();
    Object.entries(sliceMap).forEach(([k, v]) => {
        v.forEach(sliceFunc => {
            actionMap.set(sliceFunc.type, {
                func: sliceFunc.stateTransform,
                slice: k
            });
        })
    });
    return (previousState, action) => {
        if (typeof previousState === 'undefined') {
            previousState = defaultState;
        }
        const slice = actionMap.get(action.type);
        if (isDefined(slice)) {
            return Object.assign({}, previousState, {
                [slice.slice]: slice.func.call(null, previousState[slice.slice], action.payload)
            });
        }
        return previousState;
    };
}