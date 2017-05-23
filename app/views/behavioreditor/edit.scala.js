@(
  data: json.BehaviorEditorData
)(implicit messages: Messages, r: RequestHeader)

@import play.filters.csrf._
@import play.api.libs.json._
@import json.Formatting._

@shared.requireJsConfig()
@shared.jsRoutes()

define("config/behavioreditor/edit", function() {
  return {
    group: @JavaScript(Json.toJson(data.group).toString),
    builtinParamTypes: @JavaScript(Json.toJson(data.builtinParamTypes).toString),
    selectedId: @JavaScript(Json.toJson(data.maybeSelectedId).toString()),
    containerId: 'editorContainer',
    csrfToken: "@CSRF.getToken(r).map(_.value)",
    envVariables: @JavaScript(Json.toJson(data.environmentVariables).toString),
    savedAnswers: @JavaScript(Json.toJson(data.savedAnswers).toString),
    oauth2Applications: @JavaScript(Json.toJson(data.oauth2Applications).toString),
    oauth2Apis: @JavaScript(Json.toJson(data.oauth2Apis).toString),
    simpleTokenApis: @JavaScript(Json.toJson(data.simpleTokenApis).toString),
    linkedOAuth2ApplicationIds: @JavaScript(Json.toJson(data.linkedOAuth2ApplicationIds).toString)
  };
});
