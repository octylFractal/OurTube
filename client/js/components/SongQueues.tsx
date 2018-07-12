import React from "react";
import {GuildId, InternalState, SongQueue, UserId, UserSongQueues} from "../reduxish/stateInterfaces";
import {CompareResult, Immutable} from "../utils";
import {SongQueueListDisplay} from "./SongQueueListDisplay";
import {checkNotNull} from "../preconditions";
import {CardGroup} from "reactstrap";
import {connectSimple} from "../reduxish/reactReduxHelp";

export interface SongQueuesProps {
    guildId: GuildId
    submitterQueues: UserSongQueues;
}

function sortQueues(queues: UserSongQueues): Immutable.Iterable<UserId, SongQueue> {
    return queues.toSeq()
        .sort((queueA, queueB) => {
            // sort empty queues last
            const aEmpty = queueA.isEmpty();
            const bEmpty = queueB.isEmpty();
            if (aEmpty || bEmpty) {
                if (aEmpty && bEmpty) {
                    return CompareResult.SAME;
                }
                if (aEmpty /* && !bEmpty*/) {
                    return CompareResult.B_FIRST;
                }
                return CompareResult.A_FIRST;
            }

            return queueA.get(0).queueTime.getUTCMilliseconds()
                - queueB.get(0).queueTime.getUTCMilliseconds();
        });
}

export const SongQueues: React.StatelessComponent<SongQueuesProps> = function SongQueues(props) {
    const queues = sortQueues(props.submitterQueues);

    return <div style={{
        overflowX: 'hidden'
    }}>
        <CardGroup>
            {
                queues.map((value, key) => {
                    return <SongQueueListDisplay
                        key={key}
                        guildId={props.guildId}
                        submitterId={checkNotNull(key)}
                        queuedSongs={checkNotNull(value).toArray()}/>
                }).toArray()
            }
        </CardGroup>
    </div>
};

export interface LiveSongQueuesProps {
    guildId: GuildId
}

export const LiveSongQueues: React.ComponentClass<LiveSongQueuesProps> = connectSimple(
    (ourState: InternalState, ourProps: LiveSongQueuesProps) => {
        const allQueues = ourState.songQueues.get(ourProps.guildId);
        const inChannel = ourState.usersInChannels.get(ourProps.guildId);
        if (typeof allQueues === "undefined" || typeof inChannel === "undefined") {
            const map: UserSongQueues = Immutable.Map();
            return {
                submitterQueues: map
            };
        }
        return {
            submitterQueues: allQueues
                .filter((queue, subId) => inChannel.has(checkNotNull(subId)))
                .toMap()
        };
    },
    SongQueues
);
