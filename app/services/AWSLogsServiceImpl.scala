package services

import javax.inject.{Inject, Provider}

import com.amazonaws.services.logs.model.{DeleteLogGroupRequest, PutSubscriptionFilterRequest, ResourceNotFoundException}
import com.amazonaws.services.logs.{AWSLogsAsync, AWSLogsAsyncClientBuilder}
import models.Models
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.libs.ws.WSClient
import utils.JavaFutureConverter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AWSLogsServiceImpl @Inject() (
                                     val configuration: Configuration,
                                     val models: Models,
                                     val ws: WSClient,
                                     val cache: CacheApi,
                                     val dataService: DataService,
                                     val lambdaServiceProvider: Provider[AWSLambdaService]
                                   ) extends AWSLogsService {

  def lambdaService: AWSLambdaService = lambdaServiceProvider.get

  val client: AWSLogsAsync = AWSLogsAsyncClientBuilder.standard().
    withRegion(region).
    withCredentials(credentialsProvider).
    build()

  val destinationLambdaFunctionName: String = configuration.getString("logging.destination_lambda_name").get

  def deleteGroupNamed(name: String): Future[Unit] = {
    val request = new DeleteLogGroupRequest(name)
    Future {
      try {
        client.deleteLogGroup(request)
      } catch {
        case e: ResourceNotFoundException =>
      }
    }.map(_ => Unit)
  }

  def deleteGroupForLambdaFunctionNamed(name: String): Future[Unit] = {
    deleteGroupNamed(logGroupNameFor(name))
  }

  def createSubscriptionForFunctionNamed(name: String): Future[Unit] = {
    for {
      maybeDestinationFunctionArn <- lambdaService.maybeArnForFunctionNamed(destinationLambdaFunctionName)
      request <- Future.successful(
        new PutSubscriptionFilterRequest().
          withDestinationArn(maybeDestinationFunctionArn.get).
          withLogGroupName(logGroupNameFor(name))
      )
      _ <- JavaFutureConverter.javaToScala(client.putSubscriptionFilterAsync(request)).recover {
        case e: ResourceNotFoundException =>
      }
    } yield {}
  }

}
