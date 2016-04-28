package models.silhouette

import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import models._
import models.accounts.User
import scala.concurrent.Future

class UserDAOImpl @Inject() (val models: Models) extends UserDAO {
  def find(loginInfo: LoginInfo): Future[Option[User]] = {
    if (loginInfo.providerID == User.EPHEMERAL_USER_ID) {
      val userId = loginInfo.providerKey
      models.run(User.find(userId))
    } else {
      models.run(User.findByEmail(loginInfo.providerKey))
    }
  }
  def find(userId: String): Future[Option[User]] = {
    models.run(User.find(userId))
  }
  def save(user: User): Future[User] = {
    models.run(user.save)
  }
}
