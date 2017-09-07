package data

import javax.inject._

import drivers.SlickPostgresDriver.api._
import models.IDs
import models.behaviors.behavior.{BehaviorQueries, RawBehavior}
import models.behaviors.behaviorgroup.{BehaviorGroupQueries, RawBehaviorGroup}
import models.behaviors.input.{InputQueries, RawInput}
import services.DataService
import utils.FutureSequencer

import scala.concurrent.{ExecutionContext, Future}

class EnsureExportIds @Inject() (
                                  dataService: DataService,
                                  implicit val ec: ExecutionContext
                                ) {

  def ensureForGroup(group: RawBehaviorGroup): Future[Unit] = {
    if (group.maybeExportId.isDefined) {
      Future.successful({})
    } else {
      val newExportId = Some(IDs.next)
      val action = BehaviorGroupQueries.uncompiledRawFindQuery(group.id).map(_.maybeExportId).update(newExportId)
      dataService.run(action).map(_ => {})
    }
  }

  def ensureForBehaviorGroups: Unit = {
    dataService.runNow(
      dataService.run(BehaviorGroupQueries.all.result).flatMap { groups =>
        FutureSequencer.sequence(groups, ensureForGroup)
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

  def ensureForBehaviors: Unit = {
    dataService.runNow(
      dataService.run(BehaviorQueries.all.result).flatMap { behaviors =>
        FutureSequencer.sequence(behaviors, ensureForBehavior)
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

  def ensureForInputs: Unit = {
    dataService.runNow(
      dataService.run(InputQueries.all.result).flatMap { inputs =>
        FutureSequencer.sequence(inputs, ensureForInput)
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
