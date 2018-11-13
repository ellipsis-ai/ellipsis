@(
  config: ViewConfig,
  configName: String,
  moduleToLoad: String,
  data: play.api.libs.json.JsValue,
  inlineJs: Option[String] = None
)(implicit messages: Messages, r: RequestHeader)

@import play.api.libs.json._
@shared.jsRoutes();

@JavaScript(inlineJs.getOrElse(""))

let @{configName} = {};
(function() {
  const config = @JavaScript(Json.prettyPrint(data));
  @if(config.isDevelopment) {
    console.info("*** DEVELOPMENT MODE ***\n\nPage configuration for @configName:\n", config);
  }
  @{configName} = config;
})();
