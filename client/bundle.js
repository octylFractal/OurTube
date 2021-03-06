const rollup = require("rollup");
const commonjs = require('@rollup/plugin-commonjs');
const nodeResolve = require('@rollup/plugin-node-resolve');
const babel = require('rollup-plugin-babel');
const typescript = require('rollup-plugin-typescript2');
const terser = require('rollup-plugin-terser').terser;
const replace = require('@rollup/plugin-replace');
const nodent = require('rollup-plugin-nodent');
const sourceMaps = require('rollup-plugin-sourcemaps');
const process = require('process');

const dest = './dist/client.js';
const plugins = [];
plugins.push(
    typescript({
        typescript: require('typescript')
    }),
    nodent({
        exclude: 'node_modules/**', // only transpile our source code
        promises: true,
        sourcemap: 'inline'
    }),
    replace({
        'process.env.NODE_ENV': JSON.stringify(process.env['ENVIRONMENT'] || 'production')
    }),
    nodeResolve({
        jsnext: true,
        main: true
    }),
    commonjs({
        include: 'node_modules/**'
    }),
    babel({
        exclude: 'node_modules/**', // only transpile our source code
        sourceMaps: "inline"
    }),
    sourceMaps()
);
if (process.env['ENVIRONMENT'] !== 'DEV') {
    plugins.push(
        terser()
    );
}
const inputOptions = {
    input: 'js/client.tsx',
    plugins: plugins,
    external: [
        "bootstrap",
        "jquery",
        "jqueryui",
        "react",
        "react-dom",
        "redux",
        "react-redux",
        "reactstrap",
        'crypto'
    ],
};

const outputOptions = {
    format: 'iife',
    file: dest,
    name: 'ourtube',
    sourcemap: true,
    globals: {
        "jquery": "jQuery",
        "react": "React",
        "react-dom": "ReactDOM",
        "redux": "Redux",
        "react-redux": "ReactRedux",
        "reactstrap": "Reactstrap"
    },
};

async function build() {
    console.log("Bundling...");
    // create a bundle
    const bundle = await rollup.rollup(inputOptions);
    console.log("Writing...");

    // or write the bundle to disk
    await bundle.write(outputOptions);
    console.log("Done deal!");
}

async function watch() {
    const watchOptions = {
        ...inputOptions,
        output: outputOptions,
        watch: {
            chokidar: false,
            include: ['js/**']
        },
    };
    const watcher = rollup.watch(watchOptions);
    watcher.on('event', event => {
        switch (event.code) {
            case 'START':
                console.log('Rolling...');
                break;
            case 'BUNDLE_START':
                console.log('Bundling...');
                break;
            case 'BUNDLE_END':
                console.log('Bundled!');
                break;
            case 'END':
                console.log('Rolled up!');
                break;
            case 'ERROR':
                console.log('Uh oh!', event);
                break;
            case 'FATAL':
                console.log('DEATH INCARNATE: Fatal Error!', event);
                watcher.close();
                break;
            default:
                console.log('misc event', event);
        }
    });
    return watcher;
}

if (process.env['ENVIRONMENT'] === 'DEV') {
    watch();
} else {
    build();
}
