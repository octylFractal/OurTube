import {GuildId, InternalState, UserId} from "../reduxish/stateInterfaces";
import {getNickname} from "../reduxish/store";
import {optional} from "../optional";
import {React} from "../routes/rbase";
import {connectSimple} from "../reduxish/reactReduxHelp";

export interface CachingNicknameProps {
    guildId: GuildId
    userId: UserId
}

export const CachingNickname: React.ComponentClass<CachingNicknameProps> = connectSimple(
    (state: InternalState, ownProps: CachingNicknameProps) => {
        const nickname = optional(state.nicknameCaches.get(ownProps.guildId))
            .map(guildNickCache => guildNickCache.get(ownProps.userId))
            .orElse(undefined);
        if (!nickname) {
            // This will cache the data when ready, and run connect again
            getNickname(ownProps.guildId, ownProps.userId)
                .catch(err => console.error("Error attempting to cache nickname data", err));
        }
        return {
            nickname: nickname
        }
    },
    (() => {
        // Must use full component for correct type checking
        // Hopefully not a huge perf concern...
        class Nickname extends React.Component<{ nickname: string | undefined }> {
            render() {
                return this.props.nickname || '...';
            }
        }

        return Nickname;
    })()
);