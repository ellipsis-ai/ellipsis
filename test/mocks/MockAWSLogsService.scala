package mocks

import javax.inject.Inject

import com.amazonaws.services.logs.AWSLogsAsyncClient
import models.Models
import org.scalatest.mock.MockitoSugar
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.libs.ws.WSClient
import services.{AWSLogsService, DataService}

import scala.concurrent.Future

class MockAWSLogsService @Inject() (
                                     val configuration: Configuration,
                                     val models: Models,
                                     val ws: WSClient,
                                     val cache: CacheApi,
                                     val dataService: DataService
                                   ) extends AWSLogsService with MockitoSugar {

  val client: AWSLogsAsyncClient = mock[AWSLogsAsyncClient]

  def deleteGroupForLambdaFunctionNamed(name: String): Future[Unit] = Future.successful({})

  def createSubscriptionForFunctionNamed(name: String): Future[Unit] = Future.successful({})

}
