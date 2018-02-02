@(
  config: ViewConfig,
  configName: String,
  moduleToLoad: String,
  data: play.api.libs.json.JsValue
)(implicit messages: Messages, r: RequestHeader)

@import play.api.libs.json._
@shared.jsRoutes();

let @{configName} = {};
(function() {
  const config = @JavaScript(Json.prettyPrint(data));
  @if(config.isDevelopment) {
    console.info("*** DEVELOPMENT MODE ***\n\nPage configuration for @configName:\n", config);
  }
  @{configName} = config;
  window._lload('@{config.assets.getWebpackBundle("bundles/" + moduleToLoad + ".js")}');
})();
