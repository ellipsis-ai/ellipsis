package models.silhouette

import java.util.UUID
import com.mohiva.play.silhouette.api.LoginInfo
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.dbio.DBIOAction
import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import scala.concurrent.Future

class UserDAOImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends UserDAO with DAOSlick {

  import driver.api._

  def find(id: String): Future[Option[User]] = Models.run(User.find(id))

  def find(loginInfo: LoginInfo): Future[Option[User]] = {
    if (loginInfo.providerID == User.EPHEMERAL_USER_ID) {
      val userId = loginInfo.providerKey
      Models.run(User.find(userId))
    } else {
      Models.run(User.findByEmail(loginInfo.providerKey))
    }
  }

  def save(user: User) = Models.run(user.save)
}
