package models.accounts.github.profile

import com.mohiva.play.silhouette.api.LoginInfo
import slick.dbio.DBIO

import scala.concurrent.Future

trait GithubProfileService {

  def save(profile: GithubProfile): Future[GithubProfile]

  def findAction(loginInfo: LoginInfo): DBIO[Option[GithubProfile]]

  def find(loginInfo: LoginInfo): Future[Option[GithubProfile]]

}
