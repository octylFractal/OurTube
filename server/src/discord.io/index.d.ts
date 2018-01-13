/// <reference types="node"/>

declare namespace Discord {
    export class Client {
        getAudioContext(channelID: string, callback: (error: string, stream: NodeJS.WritableStream) => void): void
    }
}
