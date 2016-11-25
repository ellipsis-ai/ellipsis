package models

import json.InputData
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.input.Input
import models.team.Team
import modules.ActorModule
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import services.PostgresDataService
import support.DBMixin

class BehaviorGroupServiceSpec extends PlaySpec with DBMixin with OneAppPerSuite {

  override implicit lazy val app: Application =
    new GuiceApplicationBuilder().
      disable[ActorModule].
      build()

  val dataService = app.injector.instanceOf(classOf[PostgresDataService])

  def newSavedTeam: Team = runNow(dataService.teams.create(IDs.next))

  def newSavedInputFor(group: BehaviorGroup): Input = {
    val data = InputData(Some(IDs.next), IDs.next, None, "", false, false, Some(group.id))
    runNow(dataService.inputs.createFor(data, group.team))
  }

  def newSavedBehaviorFor(group: BehaviorGroup): Behavior = {
    runNow(dataService.behaviors.createFor(group, None, None))
  }

  def newSavedBehaviorGroupFor(team: Team): BehaviorGroup = {
    runNow(dataService.behaviorGroups.createFor("", None, team))
  }

  "BehaviorGroupService.merge" should {

    "merge a list of groups into a new group" in {
      withEmptyDB(dataService, { db =>
        val team = newSavedTeam

        val groups = 1.to(3).map { _ => newSavedBehaviorGroupFor(team) }
        groups.foreach { group =>
          1.to(3).map { _ =>
            newSavedBehaviorFor(group)
            newSavedInputFor(group)
          }
        }
        groups.foreach { group =>
          runNow(dataService.behaviors.allForGroup(group)) must have length 3
          runNow(dataService.inputs.allForGroup(group)) must have length 3
        }

        val merged = runNow(dataService.behaviorGroups.merge(groups))

        groups.foreach { group =>
          runNow(dataService.behaviors.allForGroup(group)) mustBe empty
          runNow(dataService.inputs.allForGroup(group)) mustBe empty
          runNow(dataService.behaviorGroups.find(group.id)) mustBe empty
        }
        runNow(dataService.behaviors.allForGroup(merged)) must have length 9
        runNow(dataService.inputs.allForGroup(merged)) must have length 9
        runNow(dataService.behaviorGroups.find(merged.id)) must not be empty
      })
    }

  }

}
