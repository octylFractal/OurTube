import {React} from "../routes/rbase";
import RS from "reactstrap";

export type SMProps = { toggle: () => void, children?: any, open: boolean, title: string, backdrop?: boolean | 'static' }

export function SimpleModal(props: SMProps) {
    return <RS.Modal isOpen={props.open} toggle={props.toggle} size="large" backdrop={props.backdrop}>
        <RS.ModalHeader toggle={props.toggle}>{props.title}</RS.ModalHeader>
        <RS.ModalBody>
            {props.children}
        </RS.ModalBody>
    </RS.Modal>;
}
