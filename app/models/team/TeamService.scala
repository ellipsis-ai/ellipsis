package models.team


import java.time.ZoneId
import models.accounts.user.User
import models.organization.Organization
import slick.dbio.DBIO
import scala.concurrent.Future


trait TeamService {

  def allTeams: Future[Seq[Team]]

  def allCount: Future[Int]

  def allTeamsPaged(page: Int, size: Int): Future[Seq[Team]]

  def allTeamsWithoutOrg: Future[Seq[Team]]

  def allTeamsFor(organization: Organization): Future[Seq[Team]]

  def organizationFor(team: Team): Future[Organization]

  def setNameFor(team: Team, name: String): Future[Team]

  def setTimeZoneFor(team: Team, tz: ZoneId): Future[Team]

  def setOrganizationIdFor(team: Team, organizationId: Option[String]): Future[Team]

  def findAction(id: String): DBIO[Option[Team]]

  def find(id: String): Future[Option[Team]]

  def findByName(name: String): Future[Option[Team]]

  def find(id: String, user: User): Future[Option[Team]]

  def findForInvocationToken(tokenId: String): Future[Option[Team]]

  def create(name: String): Future[Team]

  def createAction(name: String, organization: Organization): DBIO[Team]

  def create(name: String, organization: Organization): Future[Team]

  def save(team: Team): Future[Team]

  def isAdmin(team: Team): Future[Boolean]
}
