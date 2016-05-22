package services

object AWSLambdaConstants {
  val ON_SUCCESS_PARAM = "onSuccess"
  val ON_ERROR_PARAM = "onError"
  val CONTEXT_PARAM = "ellipsis"
  val HANDLER_PARAMS = Array(ON_SUCCESS_PARAM, ON_ERROR_PARAM)
  val INVOCATION_TIMEOUT_SECONDS = 10
  val TOKEN_KEY = "token"
  val API_BASE_URL_KEY = "apiBaseUrl"
}
