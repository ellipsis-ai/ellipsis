const webpack = require('webpack');
const path = require('path');

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

    // Simple scripts used on non-React pages:
    add_to_slack: './app/assets/frontend/slack/add_to_slack',

    // React loaders:
    apiTokenGenerator: './app/assets/frontend/settings/api_token_generator/loader',
    awsConfigEditor: './app/assets/frontend/settings/aws_config_editor/loader',
    behaviorEditor: './app/assets/frontend/behavior_editor/loader',
    behaviorList: './app/assets/frontend/behavior_list/loader',
    dashboard: './app/assets/frontend/dashboard/loader',
    environmentVariables: './app/assets/frontend/settings/environment_variables/loader',
    githubConfig: './app/assets/frontend/github_config/loader',
    integrationEditor: './app/assets/frontend/settings/oauth_editor/loader',
    integrationList: './app/assets/frontend/settings/integrations/loader',
    regionalSettings: './app/assets/frontend/settings/regional_settings/loader',
    scheduling: './app/assets/frontend/scheduling/loader',
    styleguideColors: './app/assets/frontend/styleguide/colors/loader',
    supportRequest: './app/assets/frontend/support/loader',

    // Monaco language workers
    'editor_worker': 'monaco-editor/esm/vs/editor/editor.worker.js',
    'ts_worker': 'monaco-editor/esm/vs/language/typescript/ts.worker'
  },
  output: {
    globalObject: "self",
    path: "",
    filename: '[name].js',
    sourceMapFilename: '[name].map',
    publicPath: '/javascripts/',
    pathinfo: false
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
        // Monaco editor workers are loaded into isolated threads:
        "editor_worker": {
          name: "editor_worker",
          chunks: "all",
          test: /[\\/]node_modules[\\/]monaco-editor[\\/]esm[\\/]vs[\\/]editor[\\/]editor.worker/
        },
        "ts_worker": {
          name: "ts_worker",
          chunks: "all",
          test: /[\\/]node_modules[\\/]monaco-editor[\\/]esm[\\/]vs[\\/]language[\\/]typescript[\\/]ts.worker/
        },

        // Commonly-loaded vendor code (do not include Monaco since it balloons too much):
        vendor: {
          name: "vendor",
          chunks: "all",
          test: /[\\/]node_modules[\\/](core-js|javascript-debounce|uuid|react|react-dom|urijs|diff|whatwg-fetch|moment|emoji-mart)[\\/]/
        }
      }
    }
  },
  plugins: [
    /* Force moment to only load English locale instead of all */
    new webpack.ContextReplacementPlugin(/moment[\/\\]locale$/, /en/)
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
