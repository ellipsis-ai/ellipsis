package models.team

import models.accounts.user.User

import scala.concurrent.Future

trait TeamService {

  def allTeams: Future[Seq[Team]]

  def allTeamsSorted: Future[Seq[Team]]

  def setInitialNameFor(team: Team, name: String): Future[Team]

  def find(id: String): Future[Option[Team]]

  def findByName(name: String): Future[Option[Team]]

  def find(id: String, user: User): Future[Option[Team]]

  def findForToken(tokenId: String): Future[Option[Team]]

  def create(name: String): Future[Team]

  def save(team: Team): Future[Team]

  def isAdmin(team: Team): Future[Boolean]
}
