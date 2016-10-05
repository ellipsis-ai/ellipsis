package support

import mocks.{MockAWSLambdaService, MockDataService, MockSlackService}
import models.IDs
import models.accounts.user.User
import models.team.Team
import modules.ActorModule
import play.api.Application
import play.api.cache.CacheApi
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import services.{AWSLambdaService, DataService, SlackService}

trait TestContext {

  def newUserFor(teamId: String): User = User(IDs.next, teamId, None)

  def appBuilder: GuiceApplicationBuilder = {
    GuiceApplicationBuilder().
      overrides(bind[SlackService].to[MockSlackService]).
      overrides(bind[DataService].to[MockDataService]).
      overrides(bind[AWSLambdaService].to[MockAWSLambdaService]).
      disable[ActorModule]
  }
  lazy val teamId: String = IDs.next
  lazy val team: Team = Team(teamId, "")
  lazy val user: User = newUserFor(teamId)
  lazy implicit val app: Application = appBuilder.build()
  lazy val dataService = app.injector.instanceOf(classOf[DataService])
  lazy val lambdaService = app.injector.instanceOf(classOf[AWSLambdaService])
  lazy val cache = app.injector.instanceOf(classOf[CacheApi])
}
