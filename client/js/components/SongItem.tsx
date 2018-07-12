import {React} from "../routes/rbase";
import {Progress} from "reactstrap";
import {GuildId, InternalState, Song, UserId} from "../reduxish/stateInterfaces";
import {NULL_THUMB, SongData} from "../reduxish/SongData";
import {optional} from "../optional";
import {connect} from "react-redux";
import {getNickname, getSongData} from "../reduxish/store";
import {Omit} from "type-zoo";
import {CachingNickname} from "./CachingNickname";

interface SongItemThumbProps {
    songData: SongData | undefined
    progress?: number
}

const SongItemThumb: React.StatelessComponent<SongItemThumbProps> = function SongItemThumb(props) {
    const thumbnail = optional(props.songData)
        .map(sd => sd.thumbnail)
        .orElse(NULL_THUMB);
    return <div className="my-2 rounded border-primary border mx-auto"
                style={{width: thumbnail.width, boxSizing: 'content-box'}}>
        <img className="" src={thumbnail.url} height={thumbnail.height} alt=""/>

        {typeof props.progress !== "undefined" &&
        <Progress animated color="warning" className="rounded-0 bg-light" value={props.progress} max={1000}/>
        }
    </div>;
};

function songDataOrFetch(state: InternalState, dataId: string): SongData | undefined {
    const songData = state.songDataCaches.get(dataId);
    if (!songData) {
        // This will cache the data when ready, and run connect again
        getSongData(dataId)
            .catch(err => console.error("Error attempting to cache song data", err));
    }
    return songData;
}

interface CachingSongItemThumbProps {
    dataId: string
    progress?: number
}

const CachingSongItemThumb: React.ComponentClass<CachingSongItemThumbProps> = connect(
    (state: InternalState, ownProps: { dataId: string }) => {
        return {
            songData: songDataOrFetch(state, ownProps.dataId)
        };
    }
)<SongItemThumbProps>(SongItemThumb);

interface CachingSongNameProps {
    dataId: string
}

type SongNameProps = Omit<CachingSongNameProps, 'dataId'> & { songData: SongData | undefined }

const CachingSongName: React.ComponentClass<CachingSongNameProps> = connect(
    (state: InternalState, ownProps: { dataId: string }) => {
        return {
            songData: songDataOrFetch(state, ownProps.dataId)
        };
    }
)(
    (() => {
        // Must use full component for correct type checking
        // Hopefully not a huge perf concern...
        class SongName extends React.Component<SongNameProps> {
            render() {
                return (this.props.songData && this.props.songData.name) || '...';
            }
        }

        return SongName;
    })()
);



export interface SongItemProps {
    guildId: string
    song: Song
    progress?: number
    submitterId?: string
}

export const SongItem: React.StatelessComponent<SongItemProps> = function SongItem(props: SongItemProps) {
    return <div className="bg-success rounded p-1">
        <CachingSongItemThumb progress={props.progress} dataId={props.song.dataId}/>
        <div className="d-flex flex-column align-items-start justify-content-start w-100 px-3">
            <h6 className="commutext text-light w-100"
                style={{
                    overflowY: 'hidden',
                    overflowX: 'hidden',
                    whiteSpace: 'nowrap',
                    textOverflow: 'ellipsis'
                }}
            ><CachingSongName dataId={props.song.dataId}/></h6>
            {typeof props.submitterId !== "undefined" &&
            <h6 className="commutext text-light w-100 small"
                style={{
                    overflowY: 'hidden',
                    overflowX: 'hidden',
                    whiteSpace: 'nowrap',
                    textOverflow: 'ellipsis'
                }}
            >
                Submitted by <CachingNickname guildId={props.guildId} userId={props.submitterId}/>
            </h6>
            }
        </div>
    </div>;
};
