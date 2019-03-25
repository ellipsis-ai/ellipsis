const config = require('config');
const baseConfigCreator = require('./webpack.base.config.js');
const webpack = require("webpack");
const devServerHost = config.get('webpack.devServer.host');
const devServerPort = config.get('webpack.devServer.port');

if (!devServerHost || !devServerPort) {
  throw new Error("You must set webpack.devServer.host and webpack.devServer.port in the shared config.");
}

module.exports = exports = function(env) {
  const webpackConfig = Object.assign({}, baseConfigCreator(env));
  webpackConfig.entry = Object.assign({}, webpackConfig.entry, {
    devServer: 'webpack/hot/dev-server',
    devServerClient: `webpack-dev-server/client?http://${devServerHost}:${devServerPort}`,
  });
  webpackConfig.output = Object.assign({}, webpackConfig.output, {
    devtoolModuleFilenameTemplate: "file://[absolute-resource-path]"
  });
  webpackConfig.mode = 'development';
  webpackConfig.devtool = false;
  webpackConfig.plugins.push(new webpack.SourceMapDevToolPlugin({
    filename: "[name].js.map",
    exclude: ["vendor.js", "editor_worker.js", "ts_worker.js"],
    publicPath: `http://${devServerHost}:${devServerPort}/javascripts/`
  }));
  return webpackConfig;
};
