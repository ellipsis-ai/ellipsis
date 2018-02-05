/* eslint no-console: "off" */
const webpack = require('webpack');
const path = require('path');

/**
 * Base configuration object for Webpack
 */
const webpackConfig = {
  entry: {
    vendor: [
      'core-js',
      'javascript-debounce',
      'node-uuid',
      'react',
      'react-dom',
      'urijs',
      'diff',
      'whatwg-fetch'
    ],
    githubConfig: './app/assets/frontend/github_config/loader',
    scheduling: './app/assets/frontend/scheduling/loader',
    styleguideColors: './app/assets/frontend/styleguide/colors/loader'
  },
  output: {
    path: "",
    filename: '[name].js',
    sourceMapFilename: '[name].map',
    publicPath: '/bundles/'
  },
  externals: {
  },
  module: {
    rules: [
      {
        test: /\.ts$/,
        use: 'ts-loader'
      },
      {
        test: /\.jsx$/,
        use: {
          loader: 'babel-loader',
          options: {
            presets: ['es2015', 'react']
          }
        },
      }
    ]
  },
  resolve: {
    extensions: ['.jsx', '.ts', '.js']
  },
  plugins: [
    new webpack.optimize.CommonsChunkPlugin({
      name: 'vendor',
      minChunks: Infinity
    })
  ]
};

module.exports = (env) => {
  const targetDir = env.WEBPACK_BUILD_PATH;
  if (!targetDir) {
    throw new Error("Must set WEBPACK_BUILD_PATH in the Webpack environment");
  } else {
    console.log(`[Webpack] Build path set to ${targetDir}`);
  }
  webpackConfig.output.path = path.resolve(__dirname, `../../../../${targetDir}`);
  return webpackConfig;
};
