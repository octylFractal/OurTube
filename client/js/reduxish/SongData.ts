export interface Thumbnail {
    url: string
    width: number
    height: number
}

export interface SongData {
    id: string
    name: string
    thumbnail: Thumbnail
}