import * as express from "express";
import {Server} from "http";
import * as socketIo from "socket.io";
import {setupApi} from "./api";
import "./discordBot";
import {shim} from "object.entries";

shim();

const app = express();
const server = new Server(app);
const io = socketIo(server, {
    path: '/transport-layer'
});


io.of('/api').on('connection', socket => {
    setupApi(socket);
});

server.listen(13445, () => console.log('Ready to go!'))
    .on('error', err => console.error(err));
