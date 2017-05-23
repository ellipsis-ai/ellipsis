@(
  teamId: String,
  apis: Seq[json.OAuth2ApiData],
  applicationId: String,
  maybeApiId: Option[String],
  maybeRecommendedScope: Option[String],
  maybeBehaviorId: Option[String]
)(implicit messages: Messages, r: RequestHeader)

@import play.filters.csrf._
@import play.api.libs.json._
@import json.Formatting._

@shared.requireJsConfig()
@shared.jsRoutes()

define("config/oauth2application/edit", function() {
  return @JavaScript(Json.prettyPrint(Json.obj(
    "containerId" -> "applicationEditor",
    "csrfToken" -> CSRF.getToken(r).map(_.value),
    "teamId" -> teamId,
    "apis" -> apis,
    "callbackUrl" -> routes.APIAccessController.linkCustomOAuth2Service(applicationId, None, None, None, None).absoluteURL(secure = true),
    "mainUrl" -> routes.ApplicationController.index().absoluteURL(secure = true),
    "applicationId" -> applicationId,
    "applicationApiId" -> maybeApiId,
    "recommendedScope" -> maybeRecommendedScope,
    "behaviorId" -> maybeBehaviorId
  )));
});
