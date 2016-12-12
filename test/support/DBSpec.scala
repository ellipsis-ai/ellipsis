package support

import com.typesafe.config.ConfigFactory
import play.api.db.Databases
import play.api.db.evolutions.Evolutions
import services.{AWSLambdaService, PostgresDataService, SlackService}
import drivers.SlickPostgresDriver.api.{Database => PostgresDatabase, _}
import json.InputData
import mocks.{MockAWSLambdaService, MockSlackService}
import models.IDs
import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.input.Input
import models.team.Team
import modules.ActorModule
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.{Application, Configuration}
import play.api.cache.CacheApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait DBSpec extends PlaySpec with OneAppPerSuite {

  lazy val config = ConfigFactory.load()
  lazy val cache = app.injector.instanceOf(classOf[CacheApi])
  lazy val configuration = app.injector.instanceOf(classOf[Configuration])

  override implicit lazy val app: Application =
    GuiceApplicationBuilder().
      overrides(bind[SlackService].to[MockSlackService]).
      overrides(bind[AWSLambdaService].to[MockAWSLambdaService]).
      disable[ActorModule].
      build()

  val dataService = app.injector.instanceOf(classOf[PostgresDataService])

  def newSavedTeam: Team = runNow(dataService.teams.create(IDs.next))

  def newSavedUserOn(team: Team): User = runNow(dataService.users.createFor(team.id))

  def newSavedInputFor(group: BehaviorGroup): Input = {
    val data = InputData(Some(IDs.next), IDs.next, None, "", false, false, Some(group.id))
    runNow(dataService.inputs.createFor(data, group.team))
  }

  def newSavedBehaviorFor(group: BehaviorGroup): Behavior = {
    runNow(dataService.behaviors.createFor(group, None, None))
  }

  def newSavedBehaviorGroupFor(team: Team): BehaviorGroup = {
    runNow(dataService.behaviorGroups.createFor("", "", None, team))
  }

  def withEmptyDB[T](dataService: PostgresDataService, fn: PostgresDatabase => T) = {
    Databases.withDatabase(
      driver = config.getString("db.default.driver"),
      url = config.getString("db.default.url"),
      config = Map(
        "username" -> config.getString("db.default.username"),
        "password" -> config.getString("db.default.password")
      )
    ) { database =>
      Evolutions.withEvolutions(database) {
        try {
          fn(dataService.models.db)
        } finally {
          // Misguided legacy down evolutions will blow up if any of these exist, so delete them
          runNow(dataService.slackProfiles.deleteAll())
          runNow(dataService.collectedParameterValues.deleteAll())
          runNow(dataService.conversations.deleteAll())
        }
      }
    }
  }

  def runNow[T](future: Future[T]): T = {
    Await.result(future, 30.seconds)
  }
}
