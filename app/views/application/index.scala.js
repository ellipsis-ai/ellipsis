@(
  config: ViewConfig,
  behaviorGroups: Seq[json.BehaviorGroupData],
  maybeSlackTeamId: Option[String],
  maybeTeamTimeZone: Option[String],
  maybeBranch: Option[String]
)(implicit messages: Messages, r: RequestHeader)

@import play.api.libs.json._
@import play.filters.csrf._
@import json.Formatting._

define(function() {
  return {
    containerId: "behaviorListContainer",
    behaviorGroups: @JavaScript(Json.asciiStringify(Json.toJson(behaviorGroups))),
    csrfToken: "@CSRF.getToken(r).map(_.value)",
    teamId: "@config.maybeTargetTeamId.getOrElse("")",
    slackTeamId: "@maybeSlackTeamId.getOrElse("")",
    teamTimeZone: @JavaScript(Json.asciiStringify(Json.toJson(maybeTeamTimeZone))),
    branchName: @JavaScript(Json.asciiStringify(Json.toJson(maybeBranch)))
  };
});
