declare module 'iso8601-duration' {
    export interface Duration {
        years: number,
        months: number,
        days: number,
        hours: number,
        minutes: number,
        seconds: number
    }

    export function toSeconds(duration: Duration, date?: Date): number

    export function parse(duration: string): Duration
}
