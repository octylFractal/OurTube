import React, {FormEvent} from "react";
import {connect} from "react-redux";
import {Actions, OUR_STORE} from "../reduxish/store";
import {SongData} from "../reduxish/SongData";
import {Button, ButtonGroup, Col, Form, FormGroup, Input, Label, Progress, Row} from 'reactstrap';
import {getApi, SongQueuedEvent} from "../websocket/api";
import {AvailableChannelSelector} from "./ChannelSelector";
import {Slider} from "./Slider";
import {InternalState, visibleEntry} from "../reduxish/stateInterfaces";

type QueuedSongData = SongData & SongQueuedEvent;

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

const SongQueueItem = (props: { guildId: string, song: QueuedSongData, progress: number }) => {
    let thumbnail = props.song.thumbnail;
    return <li className="list-group-item">
        <div className="bg-success rounded p-1">
            <div className="my-2 rounded border-primary border mx-auto"
                 style={{width: thumbnail.width, boxSizing: 'content-box'}}>
                <img className="" src={thumbnail.url} height={thumbnail.height} alt=""/>

                <Progress animated color="warning" className="rounded-0 bg-light" value={props.progress} max={1000}/>
            </div>
            <div className="d-flex flex-column align-items-start justify-content-start w-100 px-3">
                <h6 className="commutext text-light w-100"
                    style={{
                        overflowY: 'hidden',
                        overflowX: 'hidden',
                        whiteSpace: 'nowrap',
                        textOverflow: 'ellipsis'
                    }}
                >{props.song.name}</h6>
                <h6 className="commutext text-light w-100 small"
                    style={{
                        overflowY: 'hidden',
                        overflowX: 'hidden',
                        whiteSpace: 'nowrap',
                        textOverflow: 'ellipsis'
                    }}
                >Submitted by {}</h6>
            </div>
        </div>
    </li>;
};

const SongQueueListDisplay = (props: { guildId: string, queuedSongs: QueuedSongData[], songProgress?: number }) => {
    return <ul className="list-group">
        {props.queuedSongs.map((qs, index) => {
            let sp = props.songProgress;
            // multiply progress up to 1000 for precision
            const progress = (sp && index === 0) ? sp * 10 : 0;
            return <SongQueueItem key={qs.id} song={qs} progress={progress} guildId={props.guildId}/>;
        })}
    </ul>;
};

const VolumeSlider = connect((ourState: InternalState) => {
    return {
        value: visibleEntry(ourState.volumes).orElse(0)
    };
})(Slider);

interface SongControlsProps {
    guildId: string
}

function SongControls(props: SongControlsProps) {
    function skipCurrentSong() {
        const currentSong = visibleEntry(OUR_STORE.getState().currentSongs);
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
            {/*<SongQueueList/>*/}
        </Col>
    </Row>;
};
