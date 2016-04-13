package models

import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global


case class SlackBotProfile(userId: String, teamId: String, token: String)

class SlackBotProfileTable(tag: Tag) extends Table[SlackBotProfile](tag, "slack_bot_profiles") {
  def userId = column[String]("user_id", O.PrimaryKey)
  def teamId = column[String]("team_id")
  def token = column[String]("token")

  def * = (userId, teamId, token) <> ((SlackBotProfile.apply _).tupled, SlackBotProfile.unapply _)

}

object SlackBotProfileQueries {
  val all = TableQuery[SlackBotProfileTable]

  def uncompiledFindQuery(userId: Rep[String]) = {
    all.filter(_.userId === userId)
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def save(profile: SlackBotProfile): DBIO[SlackBotProfile] = {
    val query = findQuery(profile.userId)
    query.result.headOption.flatMap {
      case Some(existing) => query.update(profile)
      case None => all += profile
    }.map { number =>
      profile
    }
  }

}
