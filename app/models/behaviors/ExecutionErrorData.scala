package models.behaviors

import services.AWSLambdaLogResult

case class ExecutionErrorData(message: String, stack: String, userMessage: Option[String]) {
  def translateStack: String = AWSLambdaLogResult.translateErrors(stack)
}
