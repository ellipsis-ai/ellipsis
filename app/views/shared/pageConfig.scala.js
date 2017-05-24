@(
  configName: String,
  data: play.api.libs.json.JsValue
)(implicit messages: Messages, r: RequestHeader)

@import play.api.libs.json._

@shared.requireJsConfig()
@shared.jsRoutes()

define("@JavaScript(configName)", function() {
  return @JavaScript(Json.prettyPrint(data));
});
