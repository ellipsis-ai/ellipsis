package models.accounts.github.profile

import javax.inject.{Inject, Provider}

import com.mohiva.play.silhouette.api.LoginInfo
import services.DataService
import drivers.SlickPostgresDriver.api._

import scala.concurrent.{ExecutionContext, Future}


class GithubProfilesTable(tag: Tag) extends Table[GithubProfile](tag, "github_profiles") {
  def providerId = column[String]("provider_id")
  def providerKey = column[String]("provider_key")
  def token = column[String]("token")

  def pk = primaryKey("github_profiles_pkey", (providerId, providerKey))

  def loginInfo = (providerId, providerKey) <> (LoginInfo.tupled, LoginInfo.unapply _)

  def * = (loginInfo, token)  <> ((GithubProfile.apply _).tupled, GithubProfile.unapply _)

}

class GithubProfileServiceImpl @Inject() (
                                          dataServiceProvider: Provider[DataService],
                                          implicit val ec: ExecutionContext
                                        ) extends GithubProfileService {

  def dataService = dataServiceProvider.get

  import GithubProfileQueries._

  def save(slackProfile: GithubProfile): Future[GithubProfile] = {
    val query = findQuery(slackProfile.loginInfo.providerID, slackProfile.loginInfo.providerKey)
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

  def findAction(loginInfo: LoginInfo): DBIO[Option[GithubProfile]] = {
    findQuery(loginInfo.providerID, loginInfo.providerKey).result.headOption
  }

  def find(loginInfo: LoginInfo): Future[Option[GithubProfile]] = {
    dataService.run(findAction(loginInfo))
  }

}
