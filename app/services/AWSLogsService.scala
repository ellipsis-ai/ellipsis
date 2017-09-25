package services

import com.amazonaws.services.logs.AWSLogsAsync
import play.api.Configuration

import scala.concurrent.Future

trait AWSLogsService extends AWSService {

  val configuration: Configuration

  val client: AWSLogsAsync

  def deleteGroupForLambdaFunctionNamed(name: String): Future[Unit]
}
