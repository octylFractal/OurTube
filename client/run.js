const proxy = require('http-proxy-middleware');
const express = require('express');
const app = express();

app.use(proxy('/server', {
    target: 'http://127.0.0.1:13445',
    changeOrigin: true,
    ws: true,
    pathRewrite: {
        '^/server': '/'
    },
    logLevel: 'debug'
}));
app.use('/', express.static('dist'));

app.listen(13444, () => console.log("EJS: Ready to roll."));
