package models.behaviors.invocationlogentry

import models.behaviors.BotResult
import models.behaviors.behaviorversion.BehaviorVersion
import org.joda.time.DateTime

import scala.concurrent.Future

trait InvocationLogEntryService {

  def countsByDay: Future[Seq[(DateTime, String, Int)]]

  def createFor(
                 behaviorVersion: BehaviorVersion,
                 result: BotResult,
                 context: String,
                 maybeUserIdForContext: Option[String],
                 runtimeInMilliseconds: Long
               ): Future[InvocationLogEntry]

}
