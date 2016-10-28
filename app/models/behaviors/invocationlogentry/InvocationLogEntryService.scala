package models.behaviors.invocationlogentry

import models.behaviors.BotResult
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.MessageEvent
import org.joda.time.DateTime

import scala.concurrent.Future

trait InvocationLogEntryService {

  def countsByDay: Future[Seq[(DateTime, String, Int)]]

  def uniqueInvokingUserCountsByDay: Future[Seq[(DateTime, String, Int)]]

  def uniqueInvokedBehaviorCountsByDay: Future[Seq[(DateTime, String, Int)]]

  def createFor(
                 behaviorVersion: BehaviorVersion,
                 result: BotResult,
                 event: MessageEvent,
                 maybeUserIdForContext: Option[String],
                 runtimeInMilliseconds: Long
               ): Future[InvocationLogEntry]

}
