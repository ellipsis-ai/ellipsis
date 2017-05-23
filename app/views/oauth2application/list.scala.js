@(
  data: String
)(implicit messages: Messages, r: RequestHeader)

@shared.requireJsConfig()
@shared.jsRoutes()

define("config/oauth2application/list", function() {
  return @JavaScript(data);
});
