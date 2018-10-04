const config = require('config');
const baseConfigCreator = require('./webpack.base.config.js');
const devServerHost = config.get('webpack.devServer.host');
const devServerPort = config.get('webpack.devServer.port');

if (!devServerHost || !devServerPort) {
  throw new Error("You must set webpack.devServer.host and webpack.devServer.port in the shared config.");
}

module.exports = exports = function(env) {
  const webpackConfig = Object.assign({}, baseConfigCreator(env));
  webpackConfig.devtool = 'inline-source-map';
  webpackConfig.entry = Object.assign({}, webpackConfig.entry, {
    devServer: 'webpack/hot/dev-server',
    devServerClient: `webpack-dev-server/client?http://${devServerHost}:${devServerPort}`,
  });
  webpackConfig.output = Object.assign({}, webpackConfig.output, {
    devtoolModuleFilenameTemplate: "file://[absolute-resource-path]"
  });
  webpackConfig.mode = 'development';
  return webpackConfig;
};
