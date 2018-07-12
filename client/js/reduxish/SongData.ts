export interface Thumbnail {
    url: string
    width: number
    height: number
}

export const NULL_THUMB: Thumbnail = {
    url: 'https://i.imgur.com/3FIBbnxm.png',
    width: 320,
    height: 320
};

export interface SongData {
    id: string
    name: string
    thumbnail: Thumbnail
}