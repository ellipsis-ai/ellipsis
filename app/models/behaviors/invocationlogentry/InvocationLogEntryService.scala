package models.behaviors.invocationlogentry

import models.behaviors.BotResult
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.MessageEvent
import models.team.Team
import org.joda.time.DateTime

import scala.concurrent.Future

trait InvocationLogEntryService {

  def countsByDay: Future[Seq[(DateTime, String, Int)]]

  def uniqueInvokingUserCountsByDay: Future[Seq[(DateTime, String, Int)]]

  def uniqueInvokedBehaviorCountsByDay: Future[Seq[(DateTime, String, Int)]]

  def forTeamByDay(team: Team): Future[Seq[(DateTime, Seq[InvocationLogEntry])]]

  def createFor(
                 behaviorVersion: BehaviorVersion,
                 result: BotResult,
                 event: MessageEvent,
                 maybeUserIdForContext: Option[String],
                 runtimeInMilliseconds: Long
               ): Future[InvocationLogEntry]

}
