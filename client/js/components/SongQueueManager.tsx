import React, {FormEvent} from "react";
import {connect} from "react-redux";
import {InternalState} from "../reduxish/store";
import {SongData} from "../reduxish/SongData";
import {Button, Card, CardImg, CardImgOverlay, CardTitle, Col, Form, FormGroup, Input, Label, Row} from 'reactstrap';
import parse from "url-parse";
import {API} from "../websocket/api";
import {AvailableChannelSelector} from "./ChannelSelector";

function extractSongId(urlStr: string) {
    if (urlStr.startsWith('id:')) {
        // allow raw ID insertion, if really needed...
        return urlStr.slice('id:'.length);
    }
    const url = parse(urlStr, {}, true);
    if (url.hostname === 'www.youtube.com' && url.pathname === '/watch') {
        return url.query['v'];
    }
    if (url.hostname === 'youtu.be') {
        return url.pathname.slice(1);
    }
    // unhandled, don't know
    return undefined;
}

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
        const songId = extractSongId(String(url));
        if (!songId) {
            return;
        }
        API.queueSong(props.guildId, songId);
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

const SongQueueItem = (props: { song: SongData }) => {
    let thumbnail = props.song.thumbnail;
    return <li className="list-group-item">
        <Card style={{width: thumbnail.width}} className="m-auto border-primary border">
            <CardImg src={thumbnail.url} alt=""/>
            <CardImgOverlay>
                <CardTitle className="commutext bg-success text-light rounded">
                    {props.song.name}
                </CardTitle>
            </CardImgOverlay>
        </Card>
    </li>;
};

const SongQueueListDisplay = (props: { queuedSongs: SongData[] }) => {
    return <ul className="list-group">
        {props.queuedSongs.map(qs => <SongQueueItem key={qs.id} song={qs}/>)}
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
    return {
        queuedSongs: queuedSongs
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