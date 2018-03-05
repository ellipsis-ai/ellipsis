const webpack = require('webpack');
const config = require('config');
const devServerHost = config.get('webpack.devServer.host');
const devServerPort = config.get('webpack.devServer.port');

if (!devServerHost || !devServerPort) {
  throw new Error("You must set webpack.devServer.host and webpack.devServer.port in the shared config.");
}

module.exports = exports = (env) => {
  const webpackConfig = Object.create(require('./webpack.base.config.js')(env));
  webpackConfig.devtool = 'nosources-source-map';
  webpackConfig.entry = Object.assign({}, webpackConfig.entry, {
    devServer: 'webpack/hot/dev-server',
    devServerClient: `webpack-dev-server/client?http://${devServerHost}:${devServerPort}`,
  });
  webpackConfig.output = Object.assign({}, webpackConfig.output, {
    devtoolModuleFilenameTemplate: "file://[absolute-resource-path]"
  });
  webpackConfig.plugins = [
    new webpack.HotModuleReplacementPlugin()
  ];
  return webpackConfig;
};
