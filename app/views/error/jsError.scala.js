@(
  exception: play.api.UsefulException
)(implicit messages: Messages, r: RequestHeader)

@shared.requireJsConfig()

console.log("@{exception.title}: @{exception.description}");
document.write("<pre class='pal'>@{exception.title}: @{exception.description}\n</pre>");
document.write("<pre class='pal type-s'>Stack trace:\n@{exception.cause.getStackTrace.map(line => s"${line}\n")}</pre>");
