package models.behaviors.behaviorgroup

import models.team.Team

import scala.concurrent.Future

trait BehaviorGroupService {

  def createFor(name: String, team: Team): Future[BehaviorGroup]

}
