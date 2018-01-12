// uses youtube-dl, best library
import {spawn} from "child_process";
import {Readable} from "stream";
import {stderr, exit} from "process";

export function youtubedl(url: string): Readable {
    const proc = spawn('youtube-dl',
        ['-f', 'bestaudio', '-o', '-', url]);
    proc.stderr.pipe(stderr);
    proc.on('exit', code => {
        if (code !== 0) {
            console.error('Error in youtube-dl!!!!!!!');
            exit(1);
        }
    });
    return proc.stdout;
}
