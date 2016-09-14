package models.bots.invocationlogentry

import models.bots.BehaviorResult
import models.bots.behaviorversion.BehaviorVersion
import org.joda.time.DateTime

import scala.concurrent.Future

trait InvocationLogEntryService {

  def countsByDay: Future[Seq[(DateTime, String, Int)]]

  def createFor(
                 behaviorVersion: BehaviorVersion,
                 result: BehaviorResult,
                 context: String,
                 maybeUserIdForContext: Option[String],
                 runtimeInMilliseconds: Long
               ): Future[InvocationLogEntry]

}
