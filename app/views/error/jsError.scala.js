@(
  exception: String
)(implicit messages: Messages, r: RequestHeader)

@shared.requireJsConfig()

console.log("@exception");
