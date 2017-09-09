@(
  assets: RemoteAssets
)

requirejs.config({
  paths: {
    'common': '@assets.getUrl("javascripts/common.js").replaceFirst("\\.js$", "")'
  }
});
