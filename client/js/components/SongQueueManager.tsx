import React, {FormEvent} from "react";
import {connect} from "react-redux";
import {Actions, OUR_STORE} from "../reduxish/store";
import {Button, ButtonGroup, Col, Form, FormGroup, Input, Label, Row} from 'reactstrap';
import {getApi} from "../websocket/api";
import {AvailableChannelSelector} from "./ChannelSelector";
import {Slider} from "./Slider";
import {InternalState, visibleEntry} from "../reduxish/stateInterfaces";
import {LiveSongQueues} from "./SongQueues";

const SongAddForm = (props: { guildId: string }) => {
    const submissionHandler = (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        const $form = $(e.currentTarget);
        const $input = $form.find('#songAddField');
        const url = $input.val();
        if (!url) {
            console.log('fail', url);
            return;
        }
        $input.val('');
        getApi().then(api => api.queueSongs(props.guildId, String(url)));
    };
    return <Form onSubmit={submissionHandler}>
        <Row className="mx-3">
            <Col md={10}>
                <FormGroup>
                    <Label for="songAddField" hidden>Youtube URL</Label>
                    <Input type="text" id="songAddField" name="songAdd" placeholder="Youtube URL"/>
                </FormGroup>
            </Col>
            <Col md={2}>
                <Button color="success">Queue</Button>
            </Col>
        </Row>
    </Form>;
};

const VolumeSlider = connect((ourState: InternalState) => {
    return {
        value: visibleEntry(ourState.volumes, ourState).orElse(0)
    };
})(Slider);

interface SongControlsProps {
    guildId: string
}

function SongControls(props: SongControlsProps) {
    function skipCurrentSong() {
        const state = OUR_STORE.getState();
        const currentSong = visibleEntry(state.currentSongs, state);
        if (!currentSong.isPresent()) {
            return;
        }
        getApi().then(api => api.skipSong(props.guildId, currentSong.value.queueId));
    }

    return <ButtonGroup>
        <Button color='info' title="Remove from list"
                onClick={skipCurrentSong}>
            <i className="fa fa-fast-forward"/>
        </Button>
        <div className="btn bg-info text-light" style={{
            width: '10vw',
            cursor: 'default'
        }}>
            <VolumeSlider onValueSet={value => {
                // update local volume immediately to keep state
                OUR_STORE.dispatch(Actions.setVolume({
                    guildId: props.guildId,
                    volume: value
                }));
                getApi().then(api => api.setVolume(props.guildId, value));
            }}/>
        </div>
    </ButtonGroup>;
}

export default (props: { guildId: string }) => {
    return <Row>
        <Col md={5} className="m-auto">
            <div className="d-flex p-3 justify-content-between">
                <AvailableChannelSelector/>
                <SongControls guildId={props.guildId}/>
            </div>
            <SongAddForm guildId={props.guildId}/>
            <LiveSongQueues guildId={props.guildId}/>
        </Col>
    </Row>;
};
