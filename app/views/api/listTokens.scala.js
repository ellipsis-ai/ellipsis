@(
  teamId: String,
  tokens: Seq[json.APITokenData],
  maybeJustCreatedTokenId: Option[String]
)(implicit messages: Messages, r: RequestHeader)

@import play.filters.csrf._
@import play.api.libs.json._
@import json.Formatting._

@shared.requireJsConfig()
@shared.jsRoutes()

define("config/api/listTokens", function() {
  return {
    containerId: 'apiTokenGenerator',
    csrfToken: "@CSRF.getToken(r).map(_.value)",
    teamId: "@teamId",
    tokens: @JavaScript(Json.toJson(tokens).toString()),
    justCreatedTokenId: "@maybeJustCreatedTokenId.map(justCreatedTokenId => justCreatedTokenId).getOrElse("")"
  };
});
