package models.bots

import com.github.tototoshi.slick.PostgresJodaSupport._
import models.{IDs, Team}
import org.joda.time.DateTime
import play.api.Play
import services.AWSLambdaService
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.matching.Regex

case class Behavior(id: String, team: Team, description: String, createdAt: DateTime) {

  lazy val conf = Play.current.configuration

  def resultFor(params: Map[String, String], service: AWSLambdaService): String = {
    service.invoke(id, params)
  }

}

case class RawBehavior(id: String, teamId: String, description: String, createdAt: DateTime)

class BehaviorsTable(tag: Tag) extends Table[RawBehavior](tag, "behaviors") {

  def id = column[String]("id", O.PrimaryKey)
  def teamId = column[String]("team_id")
  def description = column[String]("description")
  def createdAt = column[DateTime]("created_at")

  def * =
    (id, teamId, description, createdAt) <> ((RawBehavior.apply _).tupled, RawBehavior.unapply _)
}

object BehaviorQueries {

  def all = TableQuery[BehaviorsTable]
  def allWithTeam = all.join(Team.all).on(_.teamId === _.id)

  def tuple2Behavior(tuple: (RawBehavior, Team)): Behavior = {
    val rawBehavior = tuple._1
    Behavior(rawBehavior.id, tuple._2, rawBehavior.description, rawBehavior.createdAt)
  }

  def uncompiledAllForTeamQuery(teamId: Rep[String]) = {
    allWithTeam.
      filter { case(behavior, team) => team.id === teamId }
  }
  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

  def allForTeam(team: Team): DBIO[Seq[Behavior]] = {
    allForTeamQuery(team.id).result.map { tuples => tuples.map(tuple2Behavior) }
  }

  def createFor(team: Team, description: String): DBIO[Behavior] = {
    val raw = RawBehavior(IDs.next, team.id, description, DateTime.now)

    (all += raw).map { _ => Behavior(raw.id, team, description, raw.createdAt) }
  }

  private def paramsIn(code: String): Array[String] = {
    """.*function\s*\(([^\)]*)\)""".r.findFirstMatchIn(code).flatMap { firstMatch =>
      firstMatch.subgroups.headOption.map { paramString =>
        paramString.split("""\s*,\s*""")
      }
    }.getOrElse(Array())
  }

  def learnFor(regex: Regex, code: String, teamId: String, lambdaService: AWSLambdaService): DBIO[Option[Behavior]] = {
    val numExpectedParams = regex.pattern.matcher("").groupCount()
    val actualParams = paramsIn(code)
    for {
      maybeTeam <- Team.find(teamId)
      maybeTrigger <- maybeTeam.map { team =>
        RegexMessageTriggerQueries.ensureFor(team, regex).map(Some(_))
      }.getOrElse(DBIO.successful(None))
    } yield {
        maybeTrigger.map { trigger =>
          lambdaService.deployFunction(trigger.behavior.id, code, actualParams)
          trigger.behavior
        }
      }
  }
}
