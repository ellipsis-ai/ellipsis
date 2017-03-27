package data

import javax.inject._

import drivers.SlickPostgresDriver.api._
import models.IDs
import models.behaviors.behavior.{BehaviorQueries, RawBehavior}
import models.behaviors.behaviorgroup.{BehaviorGroupQueries, RawBehaviorGroup}
import models.behaviors.input.{InputQueries, RawInput}
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EnsureExportIds @Inject() (dataService: DataService) {

  def ensureForGroup(group: RawBehaviorGroup): Future[Unit] = {
    if (group.maybeExportId.isDefined) {
      Future.successful({})
    } else {
      val newExportId = Some(IDs.next)
      val action = BehaviorGroupQueries.uncompiledRawFindQuery(group.id).map(_.maybeExportId).update(newExportId)
      dataService.run(action).map(_ => {})
    }
  }

  def runSequentiallyForBehaviorGroups(groups: List[RawBehaviorGroup]): Future[Unit] = {
    groups.headOption.map { group =>
      ensureForGroup(group).flatMap { _ =>
        runSequentiallyForBehaviorGroups(groups.tail)
      }
    }.getOrElse {
      Future.successful({})
    }
  }

  def ensureForBehaviorGroups: Unit = {
    dataService.runNow(
      dataService.run(BehaviorGroupQueries.all.result).flatMap { groups =>
        runSequentiallyForBehaviorGroups(groups.toList)
      }.map(_ => {})
    )
  }

  def ensureForBehavior(behavior: RawBehavior): Future[Unit] = {
    if (behavior.maybeExportId.isDefined) {
      Future.successful({})
    } else {
      val newExportId = Some(IDs.next)
      val action = BehaviorQueries.uncompiledFindRawQuery(behavior.id).map(_.maybeExportId).update(newExportId)
      dataService.run(action).map(_ => {})
    }
  }

  def runSequentiallyForBehaviors(behaviors: List[RawBehavior]): Future[Unit] = {
    behaviors.headOption.map { ea =>
      ensureForBehavior(ea).flatMap { _ =>
        runSequentiallyForBehaviors(behaviors.tail)
      }
    }.getOrElse {
      Future.successful({})
    }
  }

  def ensureForBehaviors: Unit = {
    dataService.runNow(
      dataService.run(BehaviorQueries.all.result).flatMap { behaviors =>
        runSequentiallyForBehaviors(behaviors.toList)
      }.map(_ => {})
    )
  }

  def ensureForInput(input: RawInput): Future[Unit] = {
    if (input.maybeExportId.isDefined) {
      Future.successful({})
    } else {
      val newExportId = Some(IDs.next)
      val action = InputQueries.uncompiledFindRawQuery(input.id).map(_.maybeExportId).update(newExportId)
      dataService.run(action).map(_ => {})
    }
  }

  def runSequentiallyForInputs(inputs: List[RawInput]): Future[Unit] = {
    inputs.headOption.map { ea =>
      ensureForInput(ea).flatMap { _ =>
        runSequentiallyForInputs(inputs.tail)
      }
    }.getOrElse {
      Future.successful({})
    }
  }

  def ensureForInputs: Unit = {
    dataService.runNow(
      dataService.run(InputQueries.all.result).flatMap { inputs =>
        runSequentiallyForInputs(inputs.toList)
      }.map(_ => {})
    )
  }

  def run(): Unit = {
    ensureForBehaviorGroups
    ensureForBehaviors
    ensureForInputs
  }

  run()
}
