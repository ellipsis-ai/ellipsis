@(
  teamId: String,
  apis: Seq[json.OAuth2ApiData],
  application: models.accounts.oauth2application.OAuth2Application
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
    "callbackUrl" -> routes.APIAccessController.linkCustomOAuth2Service(application.id, None, None, None, None).absoluteURL(secure = true),
    "mainUrl" -> routes.ApplicationController.index().absoluteURL(secure = true),
    "applicationId" -> application.id,
    "applicationName" -> application.name,
    "requiresAuth" -> application.api.grantType.requiresAuth,
    "applicationClientId" -> application.clientId,
    "applicationClientSecret" -> application.clientSecret,
    "applicationScope" -> application.maybeScope,
    "applicationApiId" -> application.api.id,
    "applicationSaved" -> true
  )));
});
