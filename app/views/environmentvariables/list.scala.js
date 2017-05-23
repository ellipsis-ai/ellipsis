@(
  environmentVariableJsonString: String
)(implicit messages: Messages, r: RequestHeader)

@import play.filters.csrf._

@shared.requireJsConfig()
@shared.jsRoutes()

define("config/environmentvariables/list", function() {
  return {
    containerId: 'environmentVariableList',
    csrfToken: "@CSRF.getToken(r).map(_.value)",
    data: @JavaScript(environmentVariableJsonString)
  };
});
