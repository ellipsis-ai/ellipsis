package models.bots

import models.{Team, IDs}
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.matching.Regex

case class RegexMessageTrigger(
                                id: String,
                                behavior: Behavior,
                                regex: Regex
                                ) extends Trigger {

  def paramsFor(event: Event): Map[String, String] = {
    event match {
      case e: SlackMessageEvent => {
        regex.findFirstMatchIn(e.context.message.text).map { firstMatch =>
          firstMatch.subgroups.zipWithIndex.map { case(param, i) =>
            (s"param$i", param)
          }.toMap
        }.getOrElse(Map())
      }
      case _ => Map()
    }
  }

  def isActivatedBy(event: Event): Boolean = {
    event match {
      case e: SlackMessageEvent => regex.findFirstMatchIn(e.context.message.text).nonEmpty
      case _ => false
    }
  }

}

case class RawRegexMessageTrigger(id: String, behaviorId: String, regex: String)

class RegexMessageTriggersTable(tag: Tag) extends Table[RawRegexMessageTrigger](tag, "regex_message_triggers") {

  def id = column[String]("id", O.PrimaryKey)
  def behaviorId = column[String]("behavior_id")
  def regex = column[String]("regex")

  def * =
    (id, behaviorId, regex) <> ((RawRegexMessageTrigger.apply _).tupled, RawRegexMessageTrigger.unapply _)
}

object RegexMessageTriggerQueries {

  val all = TableQuery[RegexMessageTriggersTable]
  val allWithBehaviors = all.join(BehaviorQueries.allWithTeam).on(_.behaviorId === _._1.id)

  def tuple2Trigger(tuple: (RawRegexMessageTrigger, (RawBehavior, Team))): RegexMessageTrigger = {
    RegexMessageTrigger(tuple._1.id, BehaviorQueries.tuple2Behavior(tuple._2), tuple._1.regex.r)
  }

  def uncompiledAllForTeamQuery(teamId: Rep[String]) = {
    allWithBehaviors.filter { case(trigger, (behavior, team)) => team.id === teamId}
  }
  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

  def allFor(team: Team): DBIO[Seq[RegexMessageTrigger]] = {
    allForTeamQuery(team.id).result.map(_.map(tuple2Trigger))
  }

  def allMatching(regexString: String, teamId: String): DBIO[Seq[RegexMessageTrigger]] = {
    allWithBehaviors.
      filter { case(trigger, (behavior, team)) => team.id === teamId }.
      filter { case(trigger, _) => trigger.regex === regexString }.
      result.
      map(_.map(tuple2Trigger))
  }

  def behaviorResponsesFor(event: Event, team: Team): DBIO[Seq[BehaviorResponse]] = {
    allFor(team).map { triggers =>
      triggers.filter(_.isActivatedBy(event)).map { trigger =>
        BehaviorResponse(trigger.behavior, event, trigger.paramsFor(event))
      }
    }
  }

  def ensureFor(behavior: Behavior, regex: Regex): DBIO[RegexMessageTrigger] = {
    val regexString = regex.pattern.toString
    all.
      filter(_.behaviorId === behavior.id).
      filter(_.regex === regexString).
      result.
      flatMap { r =>
        r.headOption.map { existing =>
          DBIO.successful(existing)
        }.getOrElse {
          val newRaw = RawRegexMessageTrigger(IDs.next, behavior.id, regexString)
          (all += newRaw).map(_ => newRaw)
        }.map { ensuredRaw =>
          RegexMessageTrigger(ensuredRaw.id, behavior, regex)
        }
      }
  }

  def ensureFor(team: Team, regex: Regex): DBIO[RegexMessageTrigger] = {
    val regexString = regex.pattern.toString
    allWithBehaviors.
      filter { case(trigger, (behavior, thisTeam)) => thisTeam.id === team.id }.
      filter { case(trigger, _) => trigger.regex === regexString }.
      result.
      flatMap { r =>
      r.headOption.map { existing =>
        DBIO.successful(tuple2Trigger(existing))
      }.getOrElse {
        for {
          newBehavior <- BehaviorQueries.createFor(team)
          newRawTrigger <- DBIO.successful(RawRegexMessageTrigger(IDs.next, newBehavior.id, regexString))
          _ <- all += newRawTrigger
        } yield RegexMessageTrigger(newRawTrigger.id, newBehavior, regex)
      }
    }
  }
}
