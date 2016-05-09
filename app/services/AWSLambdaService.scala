package services

import com.amazonaws.services.lambda.AWSLambdaClient
import models.bots.Behavior
import play.api.Configuration

trait AWSLambdaService extends AWSService {

  val configuration: Configuration
  val blockingClient: AWSLambdaClient

  def invoke(functionName: String, params: Map[String, String]): String

  def deleteFunction(functionName: String): Unit
  def deployFunctionFor(behavior: Behavior, code: String, params: Array[String]): Unit

}
