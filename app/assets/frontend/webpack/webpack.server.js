// @flow
/* eslint no-console: "off" */
/**
 * Webpack server for development.
 */

const webpack = require('webpack');
const WebpackDevServer = require('webpack-dev-server');
const webpackConfigLoader = require('./webpack.config.js');
const config = require('config');
const devServerHost = config.get('webpack.devServer.host');
const devServerPort = config.get('webpack.devServer.port');

if (!devServerHost || !devServerPort) {
  throw new Error("You must set webpack.devServer.host and webpack.devServer.port in the shared config.");
}

// Notify about the path where the server is running
console.log('[Webpack] Server running at location: ' + __dirname);

// First we fire up Webpack and pass in the configuration file
let bundleStart = null;
const webpackConfig = webpackConfigLoader(({ WEBPACK_BUILD_PATH: process.env.WEBPACK_BUILD_PATH }));
const compiler = webpack(webpackConfig);

// We give notice in the terminal when it starts bundling and
// set the time it started
compiler.plugin('compile', function() {
  console.log('[Webpack] Bundling...');
  bundleStart = Date.now();
});

// We also give notice when it is done compiling, including the
// time it took. Nice to have
compiler.plugin('done', function() {
  console.log('[Webpack] Bundled in ' + (bundleStart ? Date.now() - bundleStart : "?") + 'ms!');
});

const server = new WebpackDevServer(compiler, {

  // We need to tell Webpack to serve our bundled application
  // from the build path.
  publicPath: `/bundles/`,
  contentBase: false,

  // Configure hot replacement
  hot: true,

  // The rest is terminal configurations
  quiet: false,
  noInfo: true,
  stats: {
    colors: true
  }
});

// We fire up the development server and give notice in the terminal
// that we are starting the initial bundle
server.listen(devServerPort, devServerHost, function () {
  console.log('[Webpack] Bundling project, please wait...');
});
