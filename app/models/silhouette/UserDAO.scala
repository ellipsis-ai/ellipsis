package models.silhouette

import com.mohiva.play.silhouette.api.LoginInfo
import models.accounts.User

import scala.concurrent.Future

trait UserDAO {

  def find(loginInfo: LoginInfo): Future[Option[User]]

  def find(userID: String): Future[Option[User]]

  def save(user: User): Future[User]
}
