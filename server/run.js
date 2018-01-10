const liveServer = require('live-server');

liveServer.start({
    port: 13444,
    open: false,
    root: '../client/dist',
    proxy: [
        ['/server', 'http://127.0.0.1:13445']
    ]
});
