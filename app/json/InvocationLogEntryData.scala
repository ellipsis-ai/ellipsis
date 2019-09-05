package json

import java.time.OffsetDateTime

import models.behaviors.invocationlogentry.InvocationLogEntry
import services.DefaultServices
import services.caching.SuccessResultData

import scala.concurrent.{ExecutionContext, Future}

case class InvocationLogEntryData(
                                   id: String,
                                   behaviorId: String,
                                   resultType: String,
                                   messageText: String,
                                   resultText: String,
                                   context: String,
                                   maybeChannel: Option[String],
                                   maybeUserIdForContext: Option[String],
                                   maybeOriginalEventType: Option[String],
                                   runtimeInMilliseconds: Long,
                                   createdAt: OffsetDateTime,
                                   maybeUserData: Option[UserData],
                                   maybeChoices: Option[Seq[ActionChoiceData]]
                                 )

object InvocationLogEntryData {
  def from(entry: InvocationLogEntry): InvocationLogEntryData = {
    InvocationLogEntryData(
      entry.id,
      entry.behaviorVersion.behavior.id,
      entry.resultType,
      entry.messageText,
      entry.resultText,
      entry.context,
      entry.maybeChannel,
      entry.maybeUserIdForContext,
      entry.maybeOriginalEventType.map(_.toString),
      entry.runtimeInMilliseconds,
      entry.createdAt,
      None,
      None
    )
  }

  def withData(entry: InvocationLogEntry, services: DefaultServices)
              (implicit ec: ExecutionContext): Future[InvocationLogEntryData] = {
    for {
      userData <- services.dataService.users.userDataFor(entry.user, entry.behaviorVersion.team)
      maybeSuccessResult <- SuccessResultData.maybeSuccessResultFor(entry, services.dataService, services.cacheService)
    } yield {
      InvocationLogEntryData.from(entry).copy(
        maybeUserData = Some(userData),
        maybeChoices = maybeSuccessResult.map { successResult =>
          successResult.actionChoicesFor(entry.user).map(ActionChoiceData.from)
        }
      )
    }
  }
}
