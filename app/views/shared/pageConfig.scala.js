@(
  config: ViewConfig,
  jsConfigModuleName: String,
  data: play.api.libs.json.JsValue
)(implicit messages: Messages, r: RequestHeader)

@import play.api.libs.json._

@shared.requireJsConfig()
@shared.jsRoutes();

(function() {
  var config = @JavaScript(Json.prettyPrint(data));
  @if(config.isDevelopment) {
    console.info("*** DEVELOPMENT MODE ***\n\nPage configuration for @jsConfigModuleName:\n", config);
  }
  define("@JavaScript(jsConfigModuleName)", function() {
    return config;
  });
})();
