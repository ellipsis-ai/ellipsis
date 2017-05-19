requirejs.config({
  paths: {
    'common': '@RemoteAssets.getUrl("javascripts/common.js").replaceFirst("\\.js$", "")'
  }
});
