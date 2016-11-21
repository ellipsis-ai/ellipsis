package models.behaviors.behaviorgroup

import models.team.Team

import scala.concurrent.Future

trait BehaviorGroupService {

  def createFor(name: String, team: Team): Future[BehaviorGroup]

  def allFor(team: Team): Future[Seq[BehaviorGroup]]

  def merge(groups: Seq[BehaviorGroup]): Future[BehaviorGroup]

}
