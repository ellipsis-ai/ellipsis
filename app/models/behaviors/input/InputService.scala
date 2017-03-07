package models.behaviors.input

import json.InputData
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import slick.dbio.DBIO

import scala.concurrent.Future

trait InputService {

  def find(id: String): Future[Option[Input]]

  def createFor(data: InputData, behaviorGroupVersion: BehaviorGroupVersion): Future[Input]

  def ensureFor(data: InputData, behaviorGroupVersion: BehaviorGroupVersion): Future[Input]

  def allForGroupVersionAction(groupVersion: BehaviorGroupVersion): DBIO[Seq[Input]]

  def allForGroupVersion(groupVersion: BehaviorGroupVersion): Future[Seq[Input]]

  def withEnsuredExportId(input: Input): Future[Input]

  def ensureExportIdsFor(behavior: Behavior): Future[Unit]

}
