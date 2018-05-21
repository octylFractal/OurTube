import {connect} from "react-redux";
import {DiscordChannel} from "../reduxish/discord";
import React, {ChangeEvent} from "react";
import {FormEvent} from "react";
import {Button, Col, Form, FormGroup, Input, Label, Row} from 'reactstrap';
import $ from "jquery";
import {InternalState} from "../reduxish/store";
import {API} from "../websocket/api";

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

const ChannelSelector = (props: { guildId: string, channels: DiscordChannel[], selectedChannel?: string }) => {
    const submissionHandler = (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        const $form = $(e.currentTarget);
        const $input = $form.find('#channelSelectField');
        let value = $input.val();
        if (typeof value === "object" || typeof value === "number") {
            value = value.toString();
        }
        if (value === "") {
            value = undefined;
        }
        API.selectChannel(props.guildId, value);
    };
    const options = [{value: '', name: 'None'}].concat(
        props.channels.map(ch => ({value: ch.id, name: ch.name}))
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
    const guild = ISTATE.guild;
    if (typeof guild === "undefined") {
        return {channels: [], guildId: ''};
    }
    return {
        channels: guild.channels,
        guildId: guild.instance.id,
        selectedChannel: guild.selectedChannel
    };
})(ChannelSelector);