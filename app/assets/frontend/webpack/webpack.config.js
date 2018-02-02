// @flow
const webpack = require('webpack');
const config = require('config');
const devServerHost = config.get('webpack.devServer.host');
const devServerPort = config.get('webpack.devServer.port');

if (!devServerHost || !devServerPort) {
  throw new Error("You must set webpack.devServer.host and webpack.devServer.port in the shared config.");
}

module.exports = exports = Object.create(require('./webpack.base.config.js'));

exports.devtool = 'nosources-source-map';
exports.entry = Object.assign({}, exports.entry, {
  devServer: 'webpack/hot/dev-server',
  devServerClient: `webpack-dev-server/client?http://${devServerHost}:${devServerPort}`,
});
exports.output = Object.assign({}, exports.output, {
  devtoolModuleFilenameTemplate: "file://[absolute-resource-path]"
});
exports.plugins = [
  new webpack.HotModuleReplacementPlugin()
];
