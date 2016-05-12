package models.bots

import com.github.tototoshi.slick.PostgresJodaSupport._
import models.{IDs, Team}
import org.joda.time.DateTime
import play.api.Play
import services.AWSLambdaService
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.matching.Regex

case class Behavior(
                     id: String,
                     team: Team,
                     maybeDescription: Option[String],
                     maybeShortName: Option[String],
                     hasCode: Boolean,
                     createdAt: DateTime
                     ) {

  lazy val conf = Play.current.configuration

  def functionName: String = id

  def resultFor(params: Map[String, String], service: AWSLambdaService): String = {
    service.invoke(this, params)
  }

  def unlearn(lambdaService: AWSLambdaService): DBIO[Unit] = {
    lambdaService.deleteFunction(id)
    BehaviorQueries.delete(this).map(_ => Unit)
  }

  def learnCode(code: String, lambdaService: AWSLambdaService): DBIO[Seq[BehaviorParameter]] = {
    BehaviorQueries.learnCodeFor(this, code, lambdaService)
  }

  def save: DBIO[Behavior] = BehaviorQueries.save(this)

  def toRaw: RawBehavior = {
    RawBehavior(id, team.id, maybeDescription, maybeShortName, hasCode, createdAt)
  }

}

case class RawBehavior(
                        id: String,
                        teamId: String,
                        maybeDescription: Option[String],
                        maybeShortName: Option[String],
                        hasCode: Boolean,
                        createdAt: DateTime
                        )

class BehaviorsTable(tag: Tag) extends Table[RawBehavior](tag, "behaviors") {

  def id = column[String]("id", O.PrimaryKey)
  def teamId = column[String]("team_id")
  def maybeDescription = column[Option[String]]("description")
  def maybeShortName = column[Option[String]]("short_name")
  def hasCode = column[Boolean]("has_code")
  def createdAt = column[DateTime]("created_at")

  def * =
    (id, teamId, maybeDescription, maybeShortName, hasCode, createdAt) <> ((RawBehavior.apply _).tupled, RawBehavior.unapply _)
}

object BehaviorQueries {

  def all = TableQuery[BehaviorsTable]
  def allWithTeam = all.join(Team.all).on(_.teamId === _.id)

  def tuple2Behavior(tuple: (RawBehavior, Team)): Behavior = {
    val raw = tuple._1
    Behavior(raw.id, tuple._2, raw.maybeDescription, raw.maybeShortName, raw.hasCode, raw.createdAt)
  }

  def uncompiledAllForTeamQuery(teamId: Rep[String]) = {
    allWithTeam.
      filter { case(behavior, team) => team.id === teamId }
  }
  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

  def allForTeam(team: Team): DBIO[Seq[Behavior]] = {
    allForTeamQuery(team.id).result.map { tuples => tuples.map(tuple2Behavior) }
  }

  def createFor(team: Team): DBIO[Behavior] = {
    val raw = RawBehavior(IDs.next, team.id, None, None, false, DateTime.now)

    (all += raw).map { _ => Behavior(raw.id, team, raw.maybeDescription, raw.maybeShortName, raw.hasCode, raw.createdAt) }
  }

  def uncompiledFindQueryFor(id: Rep[String]) = all.filter(_.id === id)
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def save(behavior: Behavior): DBIO[Behavior] = {
    val raw = behavior.toRaw
    val query = findQueryFor(raw.id)
    query.result.flatMap { r =>
      r.headOption.map { existing =>
        query.update(raw)
      }.getOrElse(all += raw)
    }.map(_ => behavior)
  }

  def delete(behavior: Behavior): DBIO[Behavior] = {
    all.filter(_.id === behavior.id).delete.map(_ => behavior)
  }

  private def paramsIn(code: String): Array[String] = {
    """.*function\s*\(([^\)]*)\)""".r.findFirstMatchIn(code).flatMap { firstMatch =>
      firstMatch.subgroups.headOption.map { paramString =>
        paramString.split("""\s*,\s*""").filter(_.nonEmpty)
      }
    }.getOrElse(Array())
  }

  def withoutBuiltin(params: Array[String]) = params.filterNot(ea => ea == "onSuccess" || ea == "onError" || ea == "context")

  def learnCodeFor(behavior: Behavior, code: String, lambdaService: AWSLambdaService): DBIO[Seq[BehaviorParameter]] = {
    val actualParams = paramsIn(code)
    val paramsWithoutBuiltin = withoutBuiltin(actualParams)
    lambdaService.deployFunctionFor(behavior, code, paramsWithoutBuiltin)
    (for {
      b <- behavior.copy(hasCode = true).save
      params <- BehaviorParameterQueries.ensureFor(b, paramsWithoutBuiltin)
    } yield params) transactionally
  }

  def learnFor(regex: Regex, code: String, teamId: String, lambdaService: AWSLambdaService): DBIO[Option[Behavior]] = {
    val numExpectedParams = regex.pattern.matcher("").groupCount()
    val actualParams = paramsIn(code)
    val paramsWithoutBuiltin = withoutBuiltin(actualParams)
    for {
      maybeTeam <- Team.find(teamId)
      maybeTrigger <- maybeTeam.map { team =>
        RegexMessageTriggerQueries.ensureFor(team, regex).map(Some(_))
      }.getOrElse(DBIO.successful(None))
    } yield {
        maybeTrigger.map { trigger =>
          lambdaService.deployFunctionFor(trigger.behavior, code, paramsWithoutBuiltin)
          trigger.behavior
        }
      }
  }
}
