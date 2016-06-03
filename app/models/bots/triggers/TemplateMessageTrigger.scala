package models.bots.triggers

import models.bots._
import models.{IDs, Team}
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.matching.Regex

case class TemplateMessageTrigger(
                                id: String,
                                behavior: Behavior,
                                template: String
                                ) extends MessageTrigger {

  val pattern: String = template

  val regex: Regex = {
    var pattern = template
    pattern = """\{.*?\}""".r.replaceAllIn(pattern, """(\\S+)""")
    pattern = """\s+""".r.replaceAllIn(pattern, """\\s+""")
    pattern = "(?i)^" ++ pattern
    pattern.r
  }

  private val templateParamNames: Seq[String] = {
    """\{(.*?)\}""".r.findAllMatchIn(template).flatMap(_.subgroups).toSeq
  }

  protected def paramIndexMaybesFor(params: Seq[BehaviorParameter]): Seq[Option[Int]] = {
    templateParamNames.map { paramName =>
      params.find(_.name == paramName).map(_.rank - 1)
    }
  }

}

case class RawTemplateMessageTrigger(id: String, behaviorId: String, template: String)

class TemplateMessageTriggersTable(tag: Tag) extends Table[RawTemplateMessageTrigger](tag, "template_message_triggers") {

  def id = column[String]("id", O.PrimaryKey)
  def behaviorId = column[String]("behavior_id")
  def template = column[String]("template")

  def * =
    (id, behaviorId, template) <> ((RawTemplateMessageTrigger.apply _).tupled, RawTemplateMessageTrigger.unapply _)
}

object TemplateMessageTriggerQueries {

  val all = TableQuery[TemplateMessageTriggersTable]
  val allWithBehaviors = all.join(BehaviorQueries.allWithTeam).on(_.behaviorId === _._1.id)

  def tuple2Trigger(tuple: (RawTemplateMessageTrigger, (RawBehavior, Team))): TemplateMessageTrigger = {
    TemplateMessageTrigger(tuple._1.id, BehaviorQueries.tuple2Behavior(tuple._2), tuple._1.template)
  }

  def uncompiledAllForTeamQuery(teamId: Rep[String]) = {
    allWithBehaviors.filter { case(trigger, (behavior, team)) => team.id === teamId}
  }
  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

  def allFor(team: Team): DBIO[Seq[TemplateMessageTrigger]] = {
    allForTeamQuery(team.id).result.map(_.map(tuple2Trigger))
  }

  def allMatching(template: String, teamId: String): DBIO[Seq[TemplateMessageTrigger]] = {
    allWithBehaviors.
      filter { case(trigger, (behavior, team)) => team.id === teamId }.
      filter { case(trigger, _) => trigger.template === template }.
      result.
      map(_.map(tuple2Trigger))
  }

  def uncompiledAllForBehaviorQuery(behaviorId: Rep[String]) = {
    allWithBehaviors.filter { case(_, (behavior, _)) => behavior.id === behaviorId}
  }
  val allForBehaviorQuery = Compiled(uncompiledAllForBehaviorQuery _)

  def allFor(behavior: Behavior): DBIO[Seq[TemplateMessageTrigger]] = {
    allForBehaviorQuery(behavior.id).
      result.
      map(_.map(tuple2Trigger))
  }

  def ensureFor(behavior: Behavior, template: String): DBIO[TemplateMessageTrigger] = {
    all.
      filter(_.behaviorId === behavior.id).
      filter(_.template === template).
      result.
      flatMap { r =>
      r.headOption.map { existing =>
        DBIO.successful(existing)
      }.getOrElse {
        val newRaw = RawTemplateMessageTrigger(IDs.next, behavior.id, template)
        (all += newRaw).map(_ => newRaw)
      }.map { ensuredRaw =>
        TemplateMessageTrigger(ensuredRaw.id, behavior, template)
      }
    }
  }

  def deleteAllFor(behavior: Behavior): DBIO[Unit] = {
    all.filter(_.behaviorId === behavior.id).delete.map(_ => Unit)
  }

}
