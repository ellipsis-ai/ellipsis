package controllers.api.json

trait ApiMethodWithMessageInfo extends ApiMethodInfo {
  val message: String
}
