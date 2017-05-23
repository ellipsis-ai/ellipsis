@(
  teamId: String,
  apis: Seq[json.OAuth2ApiData],
  applications: Seq[json.OAuth2ApplicationData]
)(implicit messages: Messages, r: RequestHeader)

@import play.filters.csrf._
@import play.api.libs.json._
@import json.Formatting._

@shared.requireJsConfig()
@shared.jsRoutes()

define("config/oauth2application/list", function() {
  return @JavaScript(Json.prettyPrint(Json.obj(
    "containerId" -> "applicationList",
    "csrfToken" -> CSRF.getToken(r).map(_.value),
    "teamId" -> teamId,
    "apis" -> apis,
    "applications" -> applications
  )));
});
