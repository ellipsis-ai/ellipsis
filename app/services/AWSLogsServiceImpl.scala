package services

import javax.inject.Inject

import com.amazonaws.services.logs.model.{DeleteLogGroupRequest, ResourceNotFoundException}
import com.amazonaws.services.logs.{AWSLogsAsync, AWSLogsAsyncClientBuilder}
import models.Models
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AWSLogsServiceImpl @Inject() (
                                     val configuration: Configuration,
                                     val models: Models,
                                     val ws: WSClient,
                                     val cache: CacheApi,
                                     val dataService: DataService
                                   ) extends AWSLogsService {

  val client: AWSLogsAsync = AWSLogsAsyncClientBuilder.standard().
    withRegion(region).
    withCredentials(credentialsProvider).
    build()

  def deleteGroupNamed(name: String): Future[Unit] = {
    val request = new DeleteLogGroupRequest(name)
    Future {
      try {
        client.deleteLogGroup(request)
      } catch {
        case e: ResourceNotFoundException => // we expect this when creating the first time
      }
    }.map(_ => Unit)
  }

  def deleteGroupForLambdaFunctionNamed(name: String): Future[Unit] = {
    deleteGroupNamed(s"/aws/lambda/$name")
  }

}
