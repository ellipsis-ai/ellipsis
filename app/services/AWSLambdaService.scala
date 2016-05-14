package services

import com.amazonaws.services.lambda.AWSLambdaClient
import models.Models
import models.bots.Behavior
import play.api.Configuration

trait AWSLambdaService extends AWSService {

  val configuration: Configuration
  val models: Models

  val blockingClient: AWSLambdaClient

  def invoke(behavior: Behavior, params: Map[String, String]): String

  def deleteFunction(functionName: String): Unit
  def deployFunctionFor(behavior: Behavior, functionBody: String, params: Array[String]): Unit

}
