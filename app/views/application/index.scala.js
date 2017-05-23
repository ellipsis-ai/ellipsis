@(
  teamId: String,
  behaviorGroups: Seq[json.BehaviorGroupData],
  maybeSlackTeamId: Option[String],
  maybeTeamTimeZone: Option[String],
  maybeBranch: Option[String]
)(implicit messages: Messages, r: RequestHeader)

@import play.api.libs.json._
@import play.api.routing.JavaScriptReverseRouter
@import play.filters.csrf._
@import json.Formatting._

@shared.requireJsConfig()
@shared.jsRoutes()

define("config/index", function() {
  return {
    containerId: "behaviorListContainer",
    behaviorGroups: @JavaScript(Json.toJson(behaviorGroups).toString),
    csrfToken: "@CSRF.getToken(r).map(_.value)",
    teamId: "@teamId",
    slackTeamId: "@maybeSlackTeamId.getOrElse("")",
    teamTimeZone: @JavaScript(Json.toJson(maybeTeamTimeZone).toString),
    branchName: @JavaScript(Json.toJson(maybeBranch).toString)
  };
});
