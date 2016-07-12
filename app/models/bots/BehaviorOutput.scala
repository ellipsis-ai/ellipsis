package models.bots

import models.Team
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

sealed trait BehaviorOutput {
  val behaviorVersion: BehaviorVersion
}

case class SlackChannelOutput(
                               behaviorVersion: BehaviorVersion,
                               channelName: String
                               ) extends BehaviorOutput

object SlackChannelOutput {
  val outputType = "slack_channel"
}

case class RawBehaviorOutput(
                            behaviorVersionId: String,
                            outputType: String,
                            maybeChannelName: Option[String]
                              )

class BehaviorOutputsTable(tag: Tag) extends Table[RawBehaviorOutput](tag, "behavior_outputs") {

  def behaviorVersionId = column[String]("behavior_version_id")
  def outputType = column[String]("type")
  def maybeChannelName = column[Option[String]]("channel_name")

  def * = (behaviorVersionId, outputType, maybeChannelName) <> ((RawBehaviorOutput.apply _).tupled, RawBehaviorOutput.unapply _)
}

object BehaviorOutputQueries {
  val all = TableQuery[BehaviorOutputsTable]
  val allWithBehaviorVersion = all.join(BehaviorVersionQueries.allWithBehavior).on(_.behaviorVersionId === _._1.id)

  def tuple2Output(tuple: (RawBehaviorOutput, (RawBehaviorVersion, (RawBehavior, Team)))): BehaviorOutput = {
    val raw = tuple._1
    val version = BehaviorVersionQueries.tuple2BehaviorVersion(tuple._2)
    raw.outputType match {
      case(SlackChannelOutput.outputType) => SlackChannelOutput(version, raw.maybeChannelName.get)
    }
  }

  def uncompiledFindQuery(behaviorVersionId: Rep[String]) = allWithBehaviorVersion.filter(_._1.behaviorVersionId === behaviorVersionId)
  val findQuery = Compiled(uncompiledFindQuery _)

  def maybeFor(behaviorVersion: BehaviorVersion): DBIO[Option[BehaviorOutput]] = {
    findQuery(behaviorVersion.id).result.map { r => r.headOption.map(tuple2Output) }
  }
}
