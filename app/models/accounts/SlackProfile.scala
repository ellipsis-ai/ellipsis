package models.accounts

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.{SocialProfile, SocialProfileBuilder}
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global


case class SlackProfile(teamId: String, loginInfo: LoginInfo) extends SocialProfile


trait SlackProfileBuilder {
  self: SocialProfileBuilder =>

  type Profile = SlackProfile
}


class SlackProfileTable(tag: Tag) extends Table[SlackProfile](tag, "slack_profiles") {
  def providerId = column[String]("provider_id")
  def providerKey = column[String]("provider_key")
  def teamId = column[String]("team_id")

  def pk = primaryKey("slack_profiles_pkey", (providerId, providerKey))

  def loginInfo = (providerId, providerKey) <> (LoginInfo.tupled, LoginInfo.unapply _)

  def * = (teamId, loginInfo)  <> ((SlackProfile.apply _).tupled, SlackProfile.unapply _)

}

object SlackProfileQueries {
  val profiles = TableQuery[SlackProfileTable]

  private def uncompiledFindSlackProfile(providerId: Rep[String], providerKey: Rep[String]) = {
    profiles.filter(_.providerId === providerId).filter(_.providerKey === providerKey)
  }

  val findSlackProfileQuery = Compiled(uncompiledFindSlackProfile _)

  def save(slackProfile: SlackProfile): DBIO[SlackProfile] = {
    val query = findSlackProfileQuery(slackProfile.loginInfo.providerID, slackProfile.loginInfo.providerKey)
    query.result.headOption.flatMap { maybeSlackProfile =>
      maybeSlackProfile match {
        case Some(_) => query.update(slackProfile)
        case None => {
          profiles += slackProfile
        }
      }
    }.map { number =>
      slackProfile
    }
  }

  def uncompiledCountForTeam(teamId: Rep[String]) = {
    profiles.filter(_.teamId === teamId).length
  }

  val countForTeam = Compiled(uncompiledCountForTeam _)

  def countFor(teamId: String): DBIO[Int] = countForTeam(teamId).result

  def uncompiledAllForQuery(teamId: Rep[String]) = {
    profiles.filter(_.teamId === teamId)
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def allFor(teamId: String): DBIO[Seq[SlackProfile]] = {
    allForQuery(teamId).result
  }

  def find(loginInfo: LoginInfo): DBIO[Option[SlackProfile]] = {
    findSlackProfileQuery(loginInfo.providerID, loginInfo.providerKey).result.headOption
  }
}
