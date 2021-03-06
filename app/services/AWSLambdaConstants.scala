package services

object AWSLambdaConstants {
  val NO_RESPONSE_KEY = "noResponse"
  val CONTEXT_PARAM = "ellipsis"
  val SUCCESS_CALLBACK = s"$CONTEXT_PARAM.success()"
  val ERROR_CALLBACK = s"$CONTEXT_PARAM.error()"
  val API_BASE_URL_KEY = "apiBaseUrl"
  val RESULT_KEY = "successResult"
  def invocationParamFor(i: Int): String = s"param$i"

  def loadStaticJs(filename: String): String = {
    val stream = getClass.getResourceAsStream(s"/javascripts/lambda/ellipsis/$filename")
    val file = scala.io.Source.fromInputStream(stream)
    try {
      file.mkString
    } finally {
      file.close
    }
  }
  val NO_RESPONSE_CALLBACK_FUNCTION: String = {
    loadStaticJs("no_response_callback.js").
      replace("$NO_RESPONSE_KEY", NO_RESPONSE_KEY).
      replace("$CONTEXT_PARAM", CONTEXT_PARAM)
  }
  val SUCCESS_CALLBACK_FUNCTION: String = {
    loadStaticJs("success_callback.js").replace("$CONTEXT_PARAM", CONTEXT_PARAM)
  }
  val ERROR_CALLBACK_FUNCTION: String = {
    loadStaticJs("error_callback.js").replace("$CONTEXT_PARAM", CONTEXT_PARAM)
  }
  val ERROR_CLASS: String = loadStaticJs("error.js")
  val OVERRIDE_CONSOLE: String = loadStaticJs("console.js")
  val CALLBACK_FUNCTION: String = loadStaticJs("callback.js")
  val ELLIPSIS_REQUIRE_FUNCTION: String = loadStaticJs("ellipsis_require.js")

  val FETCH_FUNCTION_FOR_FILE_PARAM_NAME = "fetch_function_for_file_param"
  val FETCH_FUNCTION_FOR_FILE_PARAM: String = {
    loadStaticJs(s"$FETCH_FUNCTION_FOR_FILE_PARAM_NAME.js").replace("$CONTEXT_PARAM", CONTEXT_PARAM)
  }

  val ADD_UPLOAD_FUNCTIONS: String = loadStaticJs("add_upload_functions.js").replace("$CONTEXT_PARAM", CONTEXT_PARAM)

}
