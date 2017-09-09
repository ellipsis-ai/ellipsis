package support

import akka.actor.ActorSystem
import mocks.{MockAWSLambdaService, MockAWSLogsService, MockCacheService, MockDataService}
import models.IDs
import models.accounts.user.User
import models.behaviors.BotResultService
import models.behaviors.events.EventHandler
import models.team.Team
import modules.ActorModule
import org.scalatest.mock.MockitoSugar
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.{Application, Configuration}
import services._

import scala.concurrent.ExecutionContext

trait TestContext extends MockitoSugar{

  def newUserFor(teamId: String): User = User(IDs.next, teamId, None)

  def appBuilder: GuiceApplicationBuilder = {
    GuiceApplicationBuilder().
      overrides(bind[AWSLogsService].to[MockAWSLogsService]).
      overrides(bind[DataService].to[MockDataService]).
      overrides(bind[AWSLambdaService].to[MockAWSLambdaService]).
      overrides(bind[EventHandler].toInstance(mock[EventHandler])).
      overrides(bind[GithubService].toInstance(mock[GithubService])).
      overrides(bind[GraphQLService].toInstance(mock[GraphQLService])).
      overrides(bind[SlackEventService].toInstance(mock[SlackEventService])).
      overrides(bind[BotResultService].toInstance(mock[BotResultService])).
      overrides(bind[CacheService].to[MockCacheService]).
      disable[ActorModule]
  }
  lazy val teamId: String = IDs.next
  lazy val team: Team = Team(teamId, "", None)
  lazy val user: User = newUserFor(teamId)
  lazy implicit val app: Application = appBuilder.build()
  val dataService = app.injector.instanceOf(classOf[DataService])
  val graphQLService = app.injector.instanceOf(classOf[GraphQLService])
  lazy val actorSystem = app.injector.instanceOf(classOf[ActorSystem])
  val eventHandler = app.injector.instanceOf(classOf[EventHandler])
  val githubService = app.injector.instanceOf(classOf[GithubService])
  val lambdaService = app.injector.instanceOf(classOf[AWSLambdaService])
  val slackEventService = app.injector.instanceOf(classOf[SlackEventService])
  val botResultService = app.injector.instanceOf(classOf[BotResultService])
  val cacheService = app.injector.instanceOf(classOf[CacheService])
  val ws = app.injector.instanceOf(classOf[WSClient])
  val configuration = app.injector.instanceOf(classOf[Configuration])
  lazy val services = app.injector.instanceOf(classOf[DefaultServices])
  lazy implicit val ec = app.injector.instanceOf(classOf[ExecutionContext])

}
