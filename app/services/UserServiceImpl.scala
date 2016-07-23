package services

import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import models.Models
import models.accounts.User

import scala.concurrent.Future

class UserServiceImpl @Inject() (models: Models) extends UserService {

  def retrieve(loginInfo: LoginInfo): Future[Option[User]] = {
    if (loginInfo.providerID == User.EPHEMERAL_USER_ID) {
      val userId = loginInfo.providerKey
      models.run(User.find(userId))
    } else {
      models.run(User.findByEmail(loginInfo.providerKey))
    }
  }

  def createFor(teamId: String): Future[User] = models.run(User.createOnTeamWithId(teamId).save)
}
