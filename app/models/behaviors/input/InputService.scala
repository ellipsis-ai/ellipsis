package models.behaviors.input

import json.InputData
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import slick.dbio.DBIO

import scala.concurrent.Future

trait InputService {

  def find(id: String): Future[Option[Input]]

  def createFor(data: InputData, behaviorGroup: BehaviorGroup): Future[Input]

  def ensureFor(data: InputData, behaviorGroup: BehaviorGroup): Future[Input]

  def allForGroupAction(group: BehaviorGroup): DBIO[Seq[Input]]

  def allForGroup(group: BehaviorGroup): Future[Seq[Input]]

  def withEnsuredExportId(input: Input): Future[Input]

  def ensureExportIdsFor(behavior: Behavior): Future[Unit]

}
