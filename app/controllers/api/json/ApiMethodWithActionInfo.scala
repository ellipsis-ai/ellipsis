package controllers.api.json

trait ApiMethodWithActionInfo extends ApiMethodInfo {
  val actionName: Option[String]
  val trigger: Option[String]
}
