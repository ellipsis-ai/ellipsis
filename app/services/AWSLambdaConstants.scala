package services

object AWSLambdaConstants {
  val ON_SUCCESS_PARAM = "onSuccess"
  val ON_ERROR_PARAM = "onError"
  val CONTEXT_PARAM = "ellipsis"
  val HANDLER_PARAMS = Array(ON_SUCCESS_PARAM, ON_ERROR_PARAM)
  val INVOCATION_TIMEOUT_SECONDS = 10
  val TOKEN_KEY = "token"
  val USER_INFO_KEY = "userInfo"
  val ENV_KEY = "env"
  val API_BASE_URL_KEY = "apiBaseUrl"
  val RESULT_KEY = "successResult"
  def invocationParamFor(i: Int): String = s"param$i"
}
