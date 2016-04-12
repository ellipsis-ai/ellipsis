package models.silhouette

import com.mohiva.play.silhouette.api.LoginInfo
import models._
import scala.concurrent.Future

class UserDAOImpl extends UserDAO {
  def find(loginInfo: LoginInfo): Future[Option[User]] = {
    if (loginInfo.providerID == User.EPHEMERAL_USER_ID) {
      val userId = loginInfo.providerKey
      Models.run(User.find(userId))
    } else {
      Models.run(User.findByEmail(loginInfo.providerKey))
    }
  }
  def find(userId: String): Future[Option[User]] = {
    Models.run(User.find(userId))
  }
  def save(user: User): Future[User] = {
    Models.run(user.save)
  }
}
