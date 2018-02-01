var webpack = require('webpack');
var path = require('path');
console.log(__dirname);
var buildPath = path.resolve(__dirname, '../../../../target/web/stage/public/bundles/');
var nodeModulesPath = path.resolve(__dirname, '../../../../node_modules');

/**
 * Base configuration object for Webpack
 */
var config = {
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
        test: /\.css$/,
        use: ['style-loader','css-loader']
      },
      {
        test: /\.less$/,
        use: ['style-loader', 'css-loader', 'less-loader']
      },
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
      },
      {
        test: /\.(jpg|png)$/,
        use: 'url-loader?limit=100000'
      },
      {
        test: /\.svg$/,
        use: 'url-loader?limit=10000&mimetype=image/svg+xml'
      }
    ]
  },
  resolve: {
    extensions: ['.jsx', '.ts', '.js', '.json', '.css', '.html']
  },
  plugins: [
    new webpack.optimize.CommonsChunkPlugin({
      name: 'vendor',
      minChunks: Infinity
    })
    // new webpack.ContextReplacementPlugin(
    //   /angular(\\|\/)core(\\|\/)@angular/,
    //   path.resolve(__dirname, './app')
    // ),
    // new webpack.ProvidePlugin({
    //   'fetch': 'imports?this=>global!exports?global.fetch!whatwg-fetch'
    // })
  ]
};

module.exports = config;

