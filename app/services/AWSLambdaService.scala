package services

import com.amazonaws.services.lambda.AWSLambdaClient
import play.api.Configuration

trait AWSLambdaService {

  val configuration: Configuration
  val blockingClient: AWSLambdaClient

  def invoke(functionName: String, params: Map[String, String]): String

  def deleteFunction(functionName: String): Unit
  def deployFunction(functionName: String, code: String, params: Array[String]): Unit

}
