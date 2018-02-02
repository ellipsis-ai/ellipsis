// @flow
const webpack = require('webpack');
const path = require('path');
const buildPath = path.resolve(__dirname, '../../../../target/web/public/main/bundles');

/**
 * Base configuration object for Webpack
 */
const config = {
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
    styleguideColors: './app/assets/frontend/styleguide/colors/loader'
  },
  output: {
    path: buildPath,
    filename: '[name].js',
    sourceMapFilename: '[name].map',
    publicPath: '/assets/bundles/'
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

module.exports = config;

