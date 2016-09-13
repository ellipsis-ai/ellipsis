package services

object AWSLambdaConstants {
  val NO_RESPONSE_KEY = "noResponse"
  val CONTEXT_PARAM = "ellipsis"
  val SUCCESS_CALLBACK = s"$CONTEXT_PARAM.success()"
  val ERROR_CALLBACK = s"$CONTEXT_PARAM.error()"
  val INVOCATION_TIMEOUT_SECONDS = 10
  val TOKEN_KEY = "token"
  val USER_INFO_KEY = "userInfo"
  val ENV_KEY = "env"
  val API_BASE_URL_KEY = "apiBaseUrl"
  val RESULT_KEY = "successResult"
  def invocationParamFor(i: Int): String = s"param$i"
}
