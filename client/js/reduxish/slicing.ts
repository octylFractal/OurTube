import {newAction, OTAction} from "./actionCreators";
import {AnyAction, Reducer} from "redux";
import {oKeys, oStrKeys, StrKeys} from "../utils";

export type ActionForSliceTransform<STATE, SLICE, P> = (prevState: SLICE, payload: P, fullPrevState: STATE) => SLICE

export interface AFSForTypeFactory<P> {
    <TYPE extends string>(type: TYPE): ActionForSlice<TYPE, P>
}

export interface ActionForSlice<TYPE extends string, P> {
    (payload: P): OTAction<P>

    type: TYPE
    slice: string
}

export interface ActionForSliceFactory<STATE> {
    newAction<P, S extends StrKeys<STATE>>(
        stateSlice: S,
        transform: ActionForSliceTransform<STATE, STATE[S], P>
    ): AFSForTypeFactory<P>

    getReducer(): Reducer<STATE>
}

export type AFSTypeFactoryMap = Record<string, AFSForTypeFactory<any>>;

export type AFSMap<TM> = {
    [TYPE in StrKeys<TM>]: TM[TYPE] extends AFSForTypeFactory<infer P> ? ActionForSlice<TYPE, P> : never
}

export function buildActionMap<ATFM extends AFSTypeFactoryMap>(map: ATFM): AFSMap<ATFM> {
    const actionMap = {} as AFSMap<ATFM>;
    oStrKeys(map)
        .forEach(val => {
            actionMap[val] = map[val](val) as any;
        });
    return actionMap;
}

type TransformCollection<STATE, SLICE> = {
    [type: string]: ActionForSliceTransform<STATE, SLICE, any> | undefined
};

class AFSFactoryImpl<STATE extends { [k: string]: any }> implements ActionForSliceFactory<STATE> {
    sliceTransforms: {[S in StrKeys<STATE>]?: TransformCollection<STATE, STATE[S]>} = {};
    typeCheck = new Set<string>();

    newAction<P, S extends StrKeys<STATE>>(
        stateSlice: S,
        transform: ActionForSliceTransform<STATE, STATE[S], P>
    ) {
        const afsTypeFactory: AFSForTypeFactory<P> = (type) => {
            if (this.typeCheck.has(type)) {
                throw new Error(`Already seen type ${type}!`);
            }
            this.typeCheck.add(type);
            let transColl = this.sliceTransforms[stateSlice];
            if (typeof transColl === "undefined") {
                this.sliceTransforms[stateSlice] = transColl = {};
            }
            transColl[type] = transform;
            return Object.assign((payload: P) => newAction(type, payload),
                {
                    type: type,
                    slice: stateSlice
                });
        };
        return afsTypeFactory;
    }

    getReducer(): Reducer<STATE> {
        return (prevState, action: AnyAction) => {
            if (typeof prevState === "undefined") {
                throw new Error("State should be pre-loaded.");
            }
            const newState = Object.assign({}, prevState);
            for (let key of oKeys(this.sliceTransforms)) {
                let sliceTransform = this.sliceTransforms[key];
                if (typeof sliceTransform === "undefined") {
                    throw new Error("This can't happen.");
                }
                const reducerSlice = sliceTransform[action.type];
                if (reducerSlice) {
                    newState[key] = reducerSlice(newState[key], action.payload, prevState);
                }
            }
            return newState;
        };
    }
}

export function afsFactory<STATE>(): ActionForSliceFactory<STATE> {
    return new AFSFactoryImpl<STATE>();
}