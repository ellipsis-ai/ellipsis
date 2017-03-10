package support

import com.typesafe.config.ConfigFactory
import play.api.db.Databases
import play.api.db.evolutions.Evolutions
import services.{AWSLambdaService, PostgresDataService}
import drivers.SlickPostgresDriver.api.{Database => PostgresDatabase, _}
import json.{BehaviorGroupData, BehaviorParameterData, BehaviorParameterTypeData, InputData}
import mocks.MockAWSLambdaService
import models.IDs
import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorparameter.BehaviorParameter
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.input.Input
import models.behaviors.savedanswer.SavedAnswer
import models.behaviors.triggers.messagetrigger.MessageTrigger
import models.team.Team
import modules.ActorModule
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.{Application, Configuration}
import play.api.cache.CacheApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

trait DBSpec extends PlaySpec with OneAppPerSuite {

  lazy val config = ConfigFactory.load()
  lazy val cache = app.injector.instanceOf(classOf[CacheApi])
  lazy val configuration = app.injector.instanceOf(classOf[Configuration])

  override implicit lazy val app: Application =
    GuiceApplicationBuilder().
      overrides(bind[AWSLambdaService].to[MockAWSLambdaService]).
      disable[ActorModule].
      build()

  val dataService = app.injector.instanceOf(classOf[PostgresDataService])

  def newSavedTeam: Team = runNow(dataService.teams.create(IDs.next))

  def newSavedUserOn(team: Team): User = runNow(dataService.users.createFor(team.id))

  def newSavedInputFor(groupVersion: BehaviorGroupVersion): Input = {
    val data = InputData(Some(IDs.next), Some(IDs.next), None, IDs.next, None, "", false, false)
    runNow(dataService.inputs.createFor(data, groupVersion))
  }

  def newSavedAnswerFor(input: Input, user: User): SavedAnswer = {
    runNow(dataService.savedAnswers.ensureFor(input, "answer", user))
  }

  def newSavedParamFor(
                        version: BehaviorVersion,
                        maybeType: Option[BehaviorParameterTypeData] = None,
                        isSavedForTeam: Option[Boolean] = None,
                        isSavedForUser: Option[Boolean] = None,
                        maybeExistingInput: Option[Input] = None
                      ): BehaviorParameter = {
    val input = maybeExistingInput.map { input =>
      runNow(InputData.fromInput(input, dataService).flatMap { inputData =>
        dataService.inputs.ensureFor(inputData, version.groupVersion)
      })
    }.getOrElse {
      val inputData = InputData(Some(IDs.next), Some(IDs.next), None, "param", maybeType, "", isSavedForTeam.exists(identity), isSavedForUser.exists(identity))
      runNow(dataService.inputs.createFor(inputData, version.groupVersion))
    }
    val paramTypeData = runNow(BehaviorParameterTypeData.from(input.paramType, dataService))
    val data =
      Seq(
        BehaviorParameterData(
          input.name,
          Some(paramTypeData),
          input.question,
          Some(input.isSavedForTeam),
          Some(input.isSavedForUser),
          Some(input.inputId),
          Some(input.id),
          input.maybeExportId
        )
      )
    runNow(dataService.behaviorParameters.ensureFor(version, data)).head
  }

  def newSavedTriggerFor(version: BehaviorVersion): MessageTrigger = {
    runNow(dataService.messageTriggers.createFor(version, "foo", false, false, false))
  }

  def newSavedGroupVersionFor(group: BehaviorGroup, user: User): BehaviorGroupVersion = {
    val groupVersion = runNow(dataService.behaviorGroupVersions.createFor(group, user: User))
    val behaviors = runNow(dataService.behaviors.allForGroup(group))
    behaviors.map { ea =>
      newSavedVersionFor(ea, groupVersion)
    }
    groupVersion
  }

  def behaviorVersionFor(behavior: Behavior, groupVersion: BehaviorGroupVersion): BehaviorVersion = {
    runNow(dataService.behaviorVersions.findFor(behavior, groupVersion)).get
  }

  def newSavedVersionFor(behavior: Behavior, groupVersion: BehaviorGroupVersion): BehaviorVersion = {
    runNow(dataService.behaviorVersions.createFor(behavior, groupVersion, None, None))
  }

  def newSavedBehaviorFor(group: BehaviorGroup): Behavior = {
    runNow(dataService.behaviors.createFor(group, None, None, None))
  }

  def newSavedDataTypeFor(group: BehaviorGroup): Behavior = {
    runNow(dataService.behaviors.createFor(group, None, None, Some("Some type")))
  }

  def newSavedBehaviorGroupFor(team: Team): BehaviorGroup = {
    runNow(dataService.behaviorGroups.createFor(None, team))
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
