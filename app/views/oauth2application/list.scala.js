@(
  data: json.OAuth2ApplicationListConfig
)(implicit r: RequestHeader)

@import play.api.libs.json._
@import json.Formatting._
@shared.requireJsConfig()
@shared.jsRoutes()

define("config/oauth2application/list", function() {
  return @JavaScript(Json.prettyPrint(Json.toJson(data)));
});
