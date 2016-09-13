package models.bots.behavior

import models.accounts.user.User
import models.bots.BehaviorVersion
import models.team.Team

import scala.concurrent.Future

trait BehaviorService {

  def findWithoutAccessCheck(id: String): Future[Option[Behavior]]

  def find(id: String, user: User): Future[Option[Behavior]]

  def allForTeam(team: Team): Future[Seq[Behavior]]

  def createFor(team: Team, maybeImportedId: Option[String]): Future[Behavior]

  def delete(behavior: Behavior): Future[Behavior]

  def maybeCurrentVersionFor(behavior: Behavior): Future[Option[BehaviorVersion]]

  def unlearn(behavior: Behavior): Future[Unit]

}
