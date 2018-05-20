import React, {FormEvent} from "react";
import {connect} from "react-redux";
import {InternalState, SongProgress} from "../reduxish/store";
import {SongData} from "../reduxish/SongData";
import {Button, Col, Form, FormGroup, Input, Label, Progress, Row} from 'reactstrap';
import {API} from "../websocket/api";
import {AvailableChannelSelector} from "./ChannelSelector";

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
        API.queueSongs(props.guildId, String(url));
    };
    return <Form onSubmit={submissionHandler}>
        <Row className="mx-2">
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

function skipSong(guildId: string) {
    API.skipSong(guildId);
}

const SongQueueItem = (props: { guildId: string, song: SongData, progress: number }) => {
    let thumbnail = props.song.thumbnail;
    return <li className="list-group-item">
        <div className="bg-success rounded p-1">
            <div className="my-2 rounded border-primary border mx-auto"
                 style={{width: thumbnail.width, boxSizing: 'content-box'}}>
                <img className="" src={thumbnail.url} height={thumbnail.height} alt=""/>

                <Progress animated color="warning" className="rounded-0 bg-light" value={props.progress} max={1000}/>
            </div>
            <div className="d-flex align-items-center justify-content-center">
                <h5 className="commutext text-light mb-0">{props.song.name}</h5>
                <span className="mx-3 fa-stack fa-lg play-button" aria-hidden={true}
                      onClick={skipSong.bind(null, props.guildId)}>
                    <i className="fa fa-circle fa-stack-2x pb-background" aria-hidden={true}/>
                    <i className="fa fa-fast-forward fa-stack-1x" aria-hidden={true}/>
                </span>
            </div>
        </div>
    </li>;
};

const SongQueueListDisplay = (props: { guildId: string, queuedSongs: SongData[], songProgress?: SongProgress }) => {
    return <ul className="list-group">
        {props.queuedSongs.map(qs => {
            let sp = props.songProgress;
            // multiply progress up to 1000 for precision
            const progress = (sp && sp.songId === qs.id) ? sp.progress * 10 : 0;
            return <SongQueueItem key={qs.id} song={qs} progress={progress} guildId={props.guildId}/>;
        })}
    </ul>;
};

const SongQueueList = connect((ISTATE: InternalState) => {
    const songQueue = ISTATE.songQueue;
    const songDataCache = ISTATE.songDataCache;
    const queuedSongs = songQueue.reduce((arr: SongData[], next) => {
        const data = songDataCache[next];
        if (data) {
            return arr.concat(data);
        } else {
            return arr;
        }
    }, []);
    if (typeof ISTATE.guild === "undefined") {
        throw new Error("no u");
    }
    return {
        queuedSongs: queuedSongs,
        songProgress: ISTATE.songProgress,
        guildId: ISTATE.guild.instance.id
    };
})(SongQueueListDisplay);


export default (props: { guildId: string }) => {
    return <Row>
        <Col md={5} className="m-auto">
            <AvailableChannelSelector/>
            <SongAddForm guildId={props.guildId}/>
            <SongQueueList/>
        </Col>
    </Row>;
};