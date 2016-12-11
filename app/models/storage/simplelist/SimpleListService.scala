package models.storage.simplelist

import models.team.Team

import scala.concurrent.Future

trait SimpleListService {

  def find(id: String): Future[Option[SimpleList]]

  def createFor(team: Team, name: String): Future[SimpleList]

  def ensureFor(team: Team, name: String): Future[SimpleList]

  def allFor(team: Team): Future[Seq[SimpleList]]

}
