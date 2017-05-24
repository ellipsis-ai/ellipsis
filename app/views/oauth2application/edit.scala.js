@(
  data: json.OAuth2ApplicationEditConfig
)(implicit messages: Messages, r: RequestHeader)

@import play.filters.csrf._
@import play.api.libs.json._
@import json.Formatting._

@shared.requireJsConfig()
@shared.jsRoutes()

define("config/oauth2application/edit", function() {
  return @JavaScript(Json.prettyPrint(Json.toJson(data)));
});
