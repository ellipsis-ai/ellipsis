package support

import mocks.{MockAWSLambdaService, MockAWSLogsService, MockDataService}
import models.IDs
import models.accounts.user.User
import models.behaviors.events.EventHandler
import models.team.Team
import modules.ActorModule
import org.scalatest.mock.MockitoSugar
import play.api.cache.CacheApi
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.{Application, Configuration}
import services.{AWSLambdaService, AWSLogsService, DataService, GithubService}

trait TestContext extends MockitoSugar{

  def newUserFor(teamId: String): User = User(IDs.next, teamId, None)

  def appBuilder: GuiceApplicationBuilder = {
    GuiceApplicationBuilder().
      overrides(bind[AWSLogsService].to[MockAWSLogsService]).
      overrides(bind[DataService].to[MockDataService]).
      overrides(bind[AWSLambdaService].to[MockAWSLambdaService]).
      overrides(bind[EventHandler].toInstance(mock[EventHandler])).
      overrides(bind[GithubService].toInstance(mock[GithubService])).
      disable[ActorModule]
  }
  lazy val teamId: String = IDs.next
  lazy val team: Team = Team(teamId, "", None)
  lazy val user: User = newUserFor(teamId)
  lazy implicit val app: Application = appBuilder.build()
  lazy val dataService = app.injector.instanceOf(classOf[DataService])
  lazy val eventHandler = app.injector.instanceOf(classOf[EventHandler])
  lazy val githubService = app.injector.instanceOf(classOf[GithubService])
  lazy val lambdaService = app.injector.instanceOf(classOf[AWSLambdaService])
  lazy val cache = app.injector.instanceOf(classOf[CacheApi])
  lazy val ws = app.injector.instanceOf(classOf[WSClient])
  lazy val configuration = app.injector.instanceOf(classOf[Configuration])

}
