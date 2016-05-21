package services

import com.amazonaws.services.lambda.AWSLambdaAsyncClient
import models.Models
import models.bots.Behavior
import play.api.Configuration

import scala.concurrent.Future

trait AWSLambdaService extends AWSService {

  val configuration: Configuration
  val models: Models

  val client: AWSLambdaAsyncClient

  def invoke(behavior: Behavior, params: Map[String, String]): Future[String]

  def deleteFunction(functionName: String): Future[Unit]
  def deployFunctionFor(behavior: Behavior, functionBody: String, params: Array[String]): Future[Unit]

}
