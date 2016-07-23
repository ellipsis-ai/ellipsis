package services

import com.mohiva.play.silhouette.api.services.IdentityService
import models.accounts.User

import scala.concurrent.Future

trait UserService extends IdentityService[User] {
  def createFor(teamId: String): Future[User]
}
