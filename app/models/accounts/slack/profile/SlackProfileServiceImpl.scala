package models.accounts.slack.profile

import javax.inject.{Inject, Provider}

import com.mohiva.play.silhouette.api.LoginInfo
import services.DataService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class SlackProfileTable(tag: Tag) extends Table[SlackProfile](tag, "slack_profiles") {
  def providerId = column[String]("provider_id")
  def providerKey = column[String]("provider_key")
  def teamId = column[String]("team_id")

  def pk = primaryKey("slack_profiles_pkey", (providerId, providerKey))

  def loginInfo = (providerId, providerKey) <> (LoginInfo.tupled, LoginInfo.unapply _)

  def * = (teamId, loginInfo)  <> ((SlackProfile.apply _).tupled, SlackProfile.unapply _)

}

class SlackProfileServiceImpl @Inject() (
                                         dataServiceProvider: Provider[DataService]
                                       ) extends SlackProfileService {

  def dataService = dataServiceProvider.get

  import SlackProfileQueries._

  def save(slackProfile: SlackProfile): Future[SlackProfile] = {
    val query = findSlackProfileQuery(slackProfile.loginInfo.providerID, slackProfile.loginInfo.providerKey)
    val action = query.result.headOption.flatMap {
      case Some(_) => query.update(slackProfile)
      case None => {
        all += slackProfile
      }
    }.map { number =>
      slackProfile
    }
    dataService.run(action)
  }

  def countFor(teamId: String): Future[Int] = dataService.run(countForTeam(teamId).result)

  def allFor(teamId: String): Future[Seq[SlackProfile]] = {
    dataService.run(allForQuery(teamId).result)
  }

  def find(loginInfo: LoginInfo): Future[Option[SlackProfile]] = {
    val action = findSlackProfileQuery(loginInfo.providerID, loginInfo.providerKey).result.headOption
    dataService.run(action)
  }

  def deleteAll: Future[Unit] = {
    dataService.run(all.delete).map(_ => Unit)
  }

}
