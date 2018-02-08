/* eslint no-console: "off" */
const webpack = require('webpack');
const path = require('path');

/**
 * Base configuration object for Webpack
 */
const webpackConfig = {
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
      'whatwg-fetch'
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
    integrationEditor: './app/assets/frontend/settings/oauth2_editor/loader',
    integrationList: './app/assets/frontend/settings/integrations/loader',
    regionalSettings: './app/assets/frontend/settings/regional_settings/loader',
    scheduling: './app/assets/frontend/scheduling/loader',
    styleguideColors: './app/assets/frontend/styleguide/colors/loader'
  },
  output: {
    path: "",
    filename: '[name].js',
    sourceMapFilename: '[name].map',
    publicPath: '/javascripts/'
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
            presets: ['env', 'react']
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
      name: 'global'
    }),
    new webpack.optimize.CommonsChunkPlugin({
      names: ['jshint', 'vendor'],
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
  webpackConfig.output.path = path.resolve(targetDir);
  return webpackConfig;
};
