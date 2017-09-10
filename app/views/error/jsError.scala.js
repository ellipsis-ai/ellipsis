@(
  exception: play.api.UsefulException,
  assets: RemoteAssets
)(implicit messages: Messages, r: RequestHeader)

@shared.requireJsConfig(assets)

console.log("@{exception.title}: @{exception.description}");
console.log("Stack trace:", @{JavaScript(exception.cause.getStackTrace.mkString("[\"", "\",\"", "\"]"))});
document.write("<pre class='pal type-bold type-l'>@{exception.title}: @{exception.description}\n</pre>");
document.write("<pre class='pal type-s'>Stack trace:\n@{exception.cause.getStackTrace.mkString("\n")}</pre>");
