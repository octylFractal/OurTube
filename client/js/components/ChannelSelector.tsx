import {connect} from "react-redux";
import React, {ChangeEvent, FormEvent} from "react";
import {Button, Form, FormGroup, Input, Label} from 'reactstrap';
import $ from "jquery";
import {getApi} from "../websocket/api";
import {InternalState, RawChannelArray, visibleEntry} from "../reduxish/stateInterfaces";
import {optional} from "../optional";
import {Immutable} from "../utils";
import {checkNotNull} from "../preconditions";

interface Option {
    value: string
    name: string
}

type CSSelectProps = { id?: string, options: Option[], defaultValue?: string };
type CSSelectState = { selected: string };

class CSSelect extends React.Component<CSSelectProps, CSSelectState> {
    changeHandler: (e: ChangeEvent<HTMLInputElement>) => void;

    constructor(props: CSSelectProps) {
        super(props);

        this.changeHandler = e => {
            e.preventDefault();
            const val = e.target.value;
            this.setState(prevState => {
                return {...prevState, selected: val};
            });
        };

        this.state = {
            selected: ''
        };
    }

    componentWillReceiveProps() {
        this.setState((prevState, props) => {
            return {...prevState, selected: props.defaultValue || ''};
        });
    }

    render() {
        const options = this.props.options.map(opt => {
            return <option value={opt.value}>{opt.name}</option>
        });
        return <Input value={this.state.selected} type="select" id={this.props.id} name="channelSelect"
                      className="mx-2" onChange={this.changeHandler}>
            {options}
        </Input>;
    }
}

const ChannelSelector = (props: { guildId: string, channels: RawChannelArray, selectedChannel: string | undefined }) => {
    const submissionHandler = (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        const $form = $(e.currentTarget);
        const $input = $form.find('#channelSelectField');
        let value = $input.val();

        let channel: string | undefined = undefined;
        if (typeof value !== "undefined" && value !== "") {
            channel = value.toString();
        }

        getApi().then(api => api.selectChannel(props.guildId, channel));
    };
    const options = [{value: '', name: 'None'}].concat(
        props.channels.map(ch => {
            const chSafe = checkNotNull(ch);
            return {value: chSafe.id, name: chSafe.name};
        }).toArray()
    );
    return <Form onSubmit={submissionHandler} inline className="justify-content-center">
        <FormGroup>
            <Label for="channelSelectField" className="mx-2">Voice Channel</Label>
            <CSSelect id="channelSelectField" options={options} defaultValue={props.selectedChannel}/>
        </FormGroup>
        <Button color="success">Select</Button>
    </Form>;
};

export const AvailableChannelSelector = connect((ISTATE: InternalState) => {
    return {
        channels: visibleEntry(ISTATE.availableChannels).orElse(Immutable.List()),
        guildId: optional(ISTATE.visibleGuild).orElse(''),
        selectedChannel: visibleEntry(ISTATE.selectedChannelIds).orElse(undefined)
    };
})(ChannelSelector);