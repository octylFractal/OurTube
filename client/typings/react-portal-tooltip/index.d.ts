declare module 'react-portal-tooltip' {
    import * as React from "react";
    import {ReactNode} from "react";

    const ToolTip: React.StatelessComponent<{}>;
    export default ToolTip;

    export interface StatefulToolTipProps {
        parent: ReactNode
        position?: 'top' | 'right' | 'bottom' | 'left'
        tooltipTimeout?: number
    }

    export const StatefulToolTip: React.StatelessComponent<StatefulToolTipProps>;
}
