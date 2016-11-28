package models.behaviors.behaviorgroup

import models.team.Team

import scala.concurrent.Future

trait BehaviorGroupService {

  def createFor(name: String, maybeImportedId: Option[String], team: Team): Future[BehaviorGroup]

  def save(behaviorGroup: BehaviorGroup): Future[BehaviorGroup]

  def allFor(team: Team): Future[Seq[BehaviorGroup]]

  def find(id: String): Future[Option[BehaviorGroup]]

  def merge(groups: Seq[BehaviorGroup]): Future[BehaviorGroup]

  def delete(group: BehaviorGroup): Future[BehaviorGroup]

}
