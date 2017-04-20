package models.behaviors.input

import json.InputData
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import slick.dbio.DBIO

import scala.concurrent.Future

trait InputService {

  def findByInputIdAction(inputId: String, behaviorGroupVersion: BehaviorGroupVersion): DBIO[Option[Input]]

  def findByInputId(inputId: String, behaviorGroupVersion: BehaviorGroupVersion): Future[Option[Input]]

  def findAction(id: String): DBIO[Option[Input]]

  def createForAction(data: InputData, behaviorGroupVersion: BehaviorGroupVersion): DBIO[Input]

  def ensureForAction(data: InputData, behaviorGroupVersion: BehaviorGroupVersion): DBIO[Input]

  def allForGroupVersionAction(groupVersion: BehaviorGroupVersion): DBIO[Seq[Input]]

  def allForGroupVersion(groupVersion: BehaviorGroupVersion): Future[Seq[Input]]

}
