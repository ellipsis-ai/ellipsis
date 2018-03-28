package models.accounts.slack.profile

import javax.inject.{Inject, Provider}

import com.mohiva.play.silhouette.api.LoginInfo
import services.DataService
import drivers.SlickPostgresDriver.api._

import scala.concurrent.{ExecutionContext, Future}


class SlackProfileTable(tag: Tag) extends Table[SlackProfile](tag, "slack_profiles") {
  def providerId = column[String]("provider_id")
  def providerKey = column[String]("provider_key")
  def teamId = column[String]("team_id")

  def pk = primaryKey("slack_profiles_pkey", (providerId, providerKey))

  def loginInfo = (providerId, providerKey) <> (LoginInfo.tupled, LoginInfo.unapply _)

  def * = (teamId, loginInfo)  <> ((SlackProfile.apply _).tupled, SlackProfile.unapply _)

}

class SlackProfileServiceImpl @Inject() (
                                         dataServiceProvider: Provider[DataService],
                                         implicit val ec: ExecutionContext
                                       ) extends SlackProfileService {

  def dataService = dataServiceProvider.get

  import SlackProfileQueries._

  def save(slackProfile: SlackProfile): Future[SlackProfile] = {
    val query = findSlackProfileQuery(slackProfile.loginInfo.providerID, slackProfile.loginInfo.providerKey, slackProfile.teamId)
    val action = query.result.headOption.flatMap {
      case Some(_) => DBIO.successful({})
      case None => all += slackProfile
    }.map { _ =>
      slackProfile
    }
    dataService.run(action)
  }

  def allFor(teamId: String): Future[Seq[SlackProfile]] = {
    dataService.run(allForQuery(teamId).result)
  }

  def findAction(loginInfo: LoginInfo): DBIO[Option[SlackProfile]] = {
    findSlackProfileForLoginInfo(loginInfo.providerID, loginInfo.providerKey).result.headOption
  }

  def find(loginInfo: LoginInfo): Future[Option[SlackProfile]] = {
    dataService.run(findAction(loginInfo))
  }

  def deleteAll(): Future[Unit] = {
    dataService.run(all.delete).map(_ => Unit)
  }

}
