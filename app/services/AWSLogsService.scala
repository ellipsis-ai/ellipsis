package services

import com.amazonaws.services.logs.AWSLogsAsync
import models.Models
import play.api.Configuration

import scala.concurrent.Future

trait AWSLogsService extends AWSService {

  val configuration: Configuration
  val models: Models

  val client: AWSLogsAsync

  def deleteGroupForLambdaFunctionNamed(name: String): Future[Unit]
}
