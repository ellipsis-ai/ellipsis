@()(implicit r: RequestHeader)

var requirejs = {
  paths: {
    '../common': '@RemoteAssets.getUrl("javascripts/common.js").replaceFirst("\\.js$", "")'
  }
};

@shared.jsRoutes()
