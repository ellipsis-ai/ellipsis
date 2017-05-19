@()(implicit r: RequestHeader)

var requirejs = {
  paths: {
    '../common': '@RemoteAssets.getUrl("javascripts/common.js").replaceFirst("\\.js$", "")',

    @* Data paths need a trailing ? to avoid a .js extension *@
    'config/styleguide/colors': '@{routes.StyleguideController.colors()}?'
  }
};

@shared.jsRoutes()
