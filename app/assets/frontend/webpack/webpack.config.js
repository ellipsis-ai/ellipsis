// @flow
const webpack = require('webpack');

module.exports = exports = Object.create(require('./webpack.base.config.js'));

exports.devtool = 'nosources-source-map';
exports.entry = Object.assign({}, exports.entry, {
  devServer: 'webpack/hot/dev-server',
  devServerClient: 'webpack-dev-server/client?http://localhost:8080'
});
exports.output = Object.assign({}, exports.output, {
  devtoolModuleFilenameTemplate: "file://[absolute-resource-path]"
});
exports.plugins = [
  new webpack.HotModuleReplacementPlugin()
];
