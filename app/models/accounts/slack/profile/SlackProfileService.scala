package models.accounts.slack.profile

import com.mohiva.play.silhouette.api.LoginInfo
import slick.dbio.DBIO

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait SlackProfileService {

  def save(slackProfile: SlackProfile): Future[SlackProfile]

  def countFor(teamId: String): Future[Int]

  def allFor(teamId: String): Future[Seq[SlackProfile]]

  def findAction(loginInfo: LoginInfo): DBIO[Option[SlackProfile]]

  def find(loginInfo: LoginInfo): Future[Option[SlackProfile]]

  def maybeSlackUserId(loginInfo: LoginInfo): Future[Option[String]] = {
    find(loginInfo).map { maybeSlackProfile =>
      maybeSlackProfile.map(_.loginInfo.providerKey)
    }
  }

  def deleteAll(): Future[Unit]

}
