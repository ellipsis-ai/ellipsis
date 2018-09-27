/* eslint no-console: "off" */
const webpack = require('webpack');
const path = require('path');
const MonacoWebpackPlugin = require('monaco-editor-webpack-plugin');

/**
 * Base configuration object for Webpack
 */
const webpackConfig = {
  bail: true,
  resolve: {
    extensions: ['.tsx', '.jsx', '.ts', '.js']
  },
  entry: {
    // Used on every page, should include bootstrapping code
    global: ['./app/assets/frontend/page_header/page_header'],

    // Vendor JS used by React pages
    vendor: [
      'core-js',
      'javascript-debounce',
      'uuid',
      'react',
      'react-dom',
      'urijs',
      'diff',
      'whatwg-fetch',
      'moment'
    ],

    // JSHint loaded separately just on the skill editor
    jshint: ['jshint'],

    // Simple scripts used on non-React pages:
    add_to_slack: './app/assets/frontend/slack/add_to_slack',

    // React loaders:
    apiTokenGenerator: './app/assets/frontend/settings/api_token_generator/loader',
    awsConfigEditor: './app/assets/frontend/settings/aws_config_editor/loader',
    behaviorEditor: './app/assets/frontend/behavior_editor/loader',
    behaviorList: './app/assets/frontend/behavior_list/loader',
    environmentVariables: './app/assets/frontend/settings/environment_variables/loader',
    githubConfig: './app/assets/frontend/github_config/loader',
    integrationEditor: './app/assets/frontend/settings/oauth_editor/loader',
    integrationList: './app/assets/frontend/settings/integrations/loader',
    regionalSettings: './app/assets/frontend/settings/regional_settings/loader',
    scheduling: './app/assets/frontend/scheduling/loader',
    styleguideColors: './app/assets/frontend/styleguide/colors/loader',
    supportRequest: './app/assets/frontend/support/loader'
  },
  output: {
    path: "",
    filename: '[name].js',
    sourceMapFilename: '[name].map',
    publicPath: '/javascripts/'
  },
  module: {
    rules: [{
      test: /\.tsx?$/,
      exclude: /node_modules/,
      use: {
        loader: 'ts-loader'
      }
    }, {
      test: /\.jsx$/,
      exclude: /node_modules/,
      use: {
        loader: 'babel-loader',
        options: {
          presets: ['@babel/preset-env', '@babel/preset-react']
        }
      }
    }, {
      test: /\.css$/,
      use: ['style-loader', 'css-loader']
    }]
  },
  optimization: {
    splitChunks: {
      cacheGroups: {
        global: {
          name: "global",
          chunks: "initial"
        },
        vendor: {
          name: "vendor",
          chunks: "all",
        },
        jshint: {
          name: "jshint",
          chunks: "all"
        }
      }
    }
  },
  plugins: [
    /* Force moment to only load English locale instead of all */
    new webpack.ContextReplacementPlugin(/moment[\/\\]locale$/, /en/),

    new MonacoWebpackPlugin()
  ]
};

module.exports = function(env) {
  const targetDir = env.WEBPACK_BUILD_PATH;
  if (!targetDir) {
    throw new Error("Must set WEBPACK_BUILD_PATH in the Webpack environment");
  } else {
    console.log(`[Webpack] Build path set to ${targetDir}`);
  }
  webpackConfig.output.path = path.resolve(targetDir);
  return webpackConfig;
};
