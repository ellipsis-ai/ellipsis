package models.accounts.slack.profile

import com.mohiva.play.silhouette.api.LoginInfo
import slick.dbio.DBIO

import scala.concurrent.Future

trait SlackProfileService {

  def save(slackProfile: SlackProfile): Future[SlackProfile]

  def countFor(teamId: String): Future[Int]

  def allFor(teamId: String): Future[Seq[SlackProfile]]

  def findAction(loginInfo: LoginInfo): DBIO[Option[SlackProfile]]

  def find(loginInfo: LoginInfo): Future[Option[SlackProfile]]

  def deleteAll(): Future[Unit]

}
