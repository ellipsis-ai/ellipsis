package support

import akka.actor.ActorSystem
import mocks.{MockAWSLambdaService, MockAWSLogsService, MockDataService}
import models.IDs
import models.accounts.user.User
import models.behaviors.BotResultService
import models.behaviors.events.EventHandler
import models.team.Team
import modules.ActorModule
import org.scalatest.mock.MockitoSugar
import play.api.cache.CacheApi
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.{Application, Configuration}
import services._

trait TestContext extends MockitoSugar{

  def newUserFor(teamId: String): User = User(IDs.next, teamId, None)

  def appBuilder: GuiceApplicationBuilder = {
    GuiceApplicationBuilder().
      overrides(bind[AWSLogsService].to[MockAWSLogsService]).
      overrides(bind[DataService].to[MockDataService]).
      overrides(bind[AWSLambdaService].to[MockAWSLambdaService]).
      overrides(bind[EventHandler].toInstance(mock[EventHandler])).
      overrides(bind[GithubService].toInstance(mock[GithubService])).
      overrides(bind[SlackEventService].toInstance(mock[SlackEventService])).
      overrides(bind[BotResultService].toInstance(mock[BotResultService])).
      disable[ActorModule]
  }
  lazy val teamId: String = IDs.next
  lazy val team: Team = Team(teamId, "", None)
  lazy val user: User = newUserFor(teamId)
  lazy implicit val app: Application = appBuilder.build()
  val dataService = app.injector.instanceOf(classOf[DataService])
  val actorSystem = app.injector.instanceOf(classOf[ActorSystem])
  val eventHandler = app.injector.instanceOf(classOf[EventHandler])
  val githubService = app.injector.instanceOf(classOf[GithubService])
  val lambdaService = app.injector.instanceOf(classOf[AWSLambdaService])
  val slackEventService = app.injector.instanceOf(classOf[SlackEventService])
  val botResultService = app.injector.instanceOf(classOf[BotResultService])
  val cache = app.injector.instanceOf(classOf[CacheApi])
  val ws = app.injector.instanceOf(classOf[WSClient])
  val configuration = app.injector.instanceOf(classOf[Configuration])

}
