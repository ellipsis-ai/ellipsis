package models.accounts

import com.mohiva.play.silhouette.api.{Identity, LoginInfo}
import models.bots.{SlackMessageContext, MessageContext}
import models.{Team, IDs}
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

case class UserTeamAccess(user: User, loggedInTeam: Team, maybeTargetTeam: Option[Team], isAdminAccess: Boolean) {

  val maybeAdminAccessToTeam: Option[Team] = {
    if (isAdminAccess) {
      maybeTargetTeam
    } else {
      None
    }
  }

  val maybeAdminAccessToTeamId = maybeAdminAccessToTeam.map(_.id)

  val canAccessTargetTeam: Boolean = maybeTargetTeam.isDefined

}

case class User(
                 id: String,
                 teamId: String,
                 maybeEmail: Option[String]
                 ) extends Identity {

  def canAccess(team: Team): DBIO[Boolean] = teamAccessFor(Some(team.id)).map { teamAccess =>
    teamAccess.canAccessTargetTeam
  }

  def teamAccessFor(maybeTargetTeamId: Option[String]): DBIO[UserTeamAccess] = {
    for {
      loggedInTeam <- Team.find(teamId).map(_.get)
      maybeSlackLinkedAccount <- LinkedAccount.maybeForSlackFor(this)
      isAdmin <- maybeSlackLinkedAccount.map(_.isAdmin).getOrElse(DBIO.successful(false))
      maybeTeam <- maybeTargetTeamId.map { targetTeamId =>
        if (targetTeamId != teamId && !isAdmin) {
          DBIO.successful(None)
        } else {
          Team.find(targetTeamId)
        }
      }.getOrElse {
        Team.find(teamId)
      }
    } yield UserTeamAccess(this, loggedInTeam, maybeTeam, maybeTeam.exists(t => t.id != this.teamId))
  }

  def loginInfo: LoginInfo = LoginInfo(User.EPHEMERAL_USER_ID, id)

  def save: DBIO[User] = User.save(this)
}

class UsersTable(tag: Tag) extends Table[User](tag, "users") {

  def id = column[String]("id", O.PrimaryKey)
  def teamId = column[String]("team_id")
  def maybeEmail = column[Option[String]]("email")

  def * =
    (id, teamId, maybeEmail) <> ((User.apply _).tupled, User.unapply _)
}

object User {
  val EPHEMERAL_USER_ID = "EPHEMERAL"

  val all = TableQuery[UsersTable]

  def createOnTeamWithId(teamId: String): User = User(IDs.next, teamId, None)
  def createOnTeam(team: Team): User = createOnTeamWithId(team.id)

  def uncompiledIsEmailTakenQuery(email: Rep[String]) = {
    all.filter(_.maybeEmail === email).exists
  }
  val isEmailTakenQuery = Compiled(uncompiledIsEmailTakenQuery _)

  def isEmailTaken(email: String): DBIO[Boolean] = {
    isEmailTakenQuery(email).result
  }

  def uncompiledFindQueryFor(id: Rep[String]) = {
    all.filter(_.id === id)
  }
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def find(id: String): DBIO[Option[User]] = {
    findQueryFor(id).result.map(_.headOption)
  }

  def uncompiledFindByEmailQuery(email: Rep[String]) = all.filter(_.maybeEmail === email)
  val findByEmailQuery = Compiled(uncompiledFindByEmailQuery _)

  def findByEmail(email: String): DBIO[Option[User]] = findByEmailQuery(email).result.headOption

  def findFromMessageContext(context: MessageContext, team: Team): DBIO[Option[User]] = {
    context match {
      case mc: SlackMessageContext => LinkedAccount.find(LoginInfo(mc.name, mc.userIdForContext), team.id).map { maybeLinked =>
        maybeLinked.map(_.user)
      }
      case _ => DBIO.successful(None)
    }
  }

  def delete(user: User): DBIO[Option[User]] = {
    findQueryFor(user.id).delete.map{ _ => Some(user) }
  }

  def save(user: User): DBIO[User] = {
    val query = findQueryFor(user.id)
    query.result.flatMap { result =>
      result.headOption.map { existing =>
        all.filter(_.id === user.id).update(user)
      }.getOrElse {
        all += user
      }.map { _ => user }
    }
  }
}
