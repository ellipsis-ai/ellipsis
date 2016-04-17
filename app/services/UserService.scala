package services

import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import models.accounts.User

import scala.concurrent.Future

trait UserService extends IdentityService[User] {

  def save(user: User): Future[User]

  def save(profile: CommonSocialProfile): Future[User]
}
