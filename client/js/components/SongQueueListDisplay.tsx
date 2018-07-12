import {React} from "../routes/rbase";
import {GuildId, InternalState, Song, UserId} from "../reduxish/stateInterfaces";
import {Card, CardHeader, ListGroup, ListGroupItem} from "reactstrap";
import {CachingNickname} from "./CachingNickname";
import {SongItem} from "./SongItem";
import {optional} from "../optional";
import {connectSimple} from "../reduxish/reactReduxHelp";

export interface SongQueueListDisplayProps {
    guildId: GuildId
    submitterId: UserId
    queuedSongs: Song[]
}

export const SongQueueListDisplay: React.StatelessComponent<SongQueueListDisplayProps> = (props) => {
    return <Card>
        <CardHeader>
            Queue for <CachingNickname guildId={props.guildId} userId={props.submitterId}/>
        </CardHeader>
        <ListGroup flush>
            {props.queuedSongs.map((qs) => {
                return <ListGroupItem key={qs.queueId}>
                    <SongItem guildId={props.guildId} song={qs}/>
                </ListGroupItem>
            })}
        </ListGroup>
    </Card>;
};

export interface LiveSongQueueListDisplayProps {
    guildId: GuildId
    submitterId: UserId
}

export const LiveSongQueueListDisplay: React.ComponentClass<LiveSongQueueListDisplayProps> = connectSimple(
    (state: InternalState, ownProps: LiveSongQueueListDisplayProps) => {
        return {
            queuedSongs: optional(state.songQueues.get(ownProps.guildId))
                .map(guildQueue => guildQueue.get(ownProps.submitterId))
                .map(immList => immList.toArray())
                .orElse([])
        };
    },
    SongQueueListDisplay
);
