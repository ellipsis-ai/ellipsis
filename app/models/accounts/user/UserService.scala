package models.accounts.user

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import models.Team
import models.bots.events.MessageContext

import scala.concurrent.Future

trait UserService extends IdentityService[User] {
  def find(id: String): Future[Option[User]]
  def findFromMessageContext(context: MessageContext, team: Team): Future[Option[User]]
  def createFor(teamId: String): Future[User]
  def save(user: User): Future[User]
  def ensureUserFor(loginInfo: LoginInfo, teamId: String): Future[User]
}
