package models.accounts.user

import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import models.accounts.LinkedAccount
import models.bots.events.{MessageContext, SlackMessageContext}
import models.{IDs, Models, Team}
import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserServiceImpl @Inject() (models: Models) extends UserService {

  import UserQueries._

  def retrieve(loginInfo: LoginInfo): Future[Option[User]] = {
    if (loginInfo.providerID == User.EPHEMERAL_USER_ID) {
      val userId = loginInfo.providerKey
      find(userId)
    } else {
      Future.successful(None)
    }
  }

  def find(id: String): Future[Option[User]] = {
    models.run(findQueryFor(id).result.map(_.headOption))
  }

  def findFromMessageContext(context: MessageContext, team: Team): Future[Option[User]] = {
    val action = context match {
      case mc: SlackMessageContext => LinkedAccount.find(LoginInfo(mc.name, mc.userIdForContext), team.id).map { maybeLinked =>
        maybeLinked.map(_.user)
      }
      case _ => DBIO.successful(None)
    }
    models.run(action)
  }

  def createOnTeamWithId(teamId: String): User = User(IDs.next, teamId, None)
  def createOnTeam(team: Team): User = createOnTeamWithId(team.id)

  def createFor(teamId: String): Future[User] = save(createOnTeamWithId(teamId))

  private def saveAction(user: User): DBIO[User] = {
    val query = findQueryFor(user.id)
    query.result.flatMap { result =>
      result.headOption.map { existing =>
        all.filter(_.id === user.id).update(user)
      }.getOrElse {
        all += user
      }.map { _ => user }
    }
  }

  def save(user: User): Future[User] = {
    models.run(saveAction(user))
  }

  def ensureUserFor(loginInfo: LoginInfo, teamId: String): Future[User] = {
    val action = LinkedAccount.find(loginInfo, teamId).flatMap { maybeLinkedAccount =>
      maybeLinkedAccount.map(DBIO.successful).getOrElse {
        saveAction(createOnTeamWithId(teamId)).flatMap { user =>
          LinkedAccount(user, loginInfo, DateTime.now).save
        }
      }.map(_.user)
    } transactionally

    models.run(action)
  }
}
