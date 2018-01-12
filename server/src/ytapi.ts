import * as google from "googleapis";
import {secrets} from "./secrets";
import {SongData} from "./api";
import {toSeconds, parse} from "iso8601-duration";

const youtube = google.youtube('v3');

export interface Thumbnail {
    url: string
    width: number
    height: number
}

export interface Thumbnails {
    default: Thumbnail
    medium: Thumbnail
    high: Thumbnail
    standard: Thumbnail
    maxres: Thumbnail
}

export interface Snippet {
    title: string
    thumbnails: Thumbnails
}

export interface ContentDetails {
    duration: string
}

export function getVideoData(songId: string): Promise<SongData> {
    return new Promise<SongData>((resolve, reject) => {
        youtube.videos.list({
            auth: secrets.YT_API_KEY,
            id: songId,
            part: 'snippet,contentDetails'
        }, (error: any, data: any) => {
            if (error) {
                reject(error);
            } else {
                const items: { snippet: Snippet, contentDetails: ContentDetails }[] = data.items;
                if (items.length === 0) {
                    reject("No items, invalid id?");
                    return;
                }
                const snip = items[0].snippet;
                const content = items[0].contentDetails;
                resolve({
                    id: songId,
                    name: snip.title,
                    thumbnail: snip.thumbnails.medium,
                    duration: toSeconds(parse(content.duration)) * 1000
                });
            }
        });
    });
}