package models.behaviors

import services.AWSLambdaLogResult

case class ExecutionErrorData(message: String, stack: String, userMessage: Option[String]) {
  def translateStack(functionLines: Int): String = AWSLambdaLogResult.translateErrors(functionLines, stack)
}
