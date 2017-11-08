package json

import java.time.OffsetDateTime

import com.mohiva.play.silhouette.api.LoginInfo
import models.behaviors.invocationlogentry.InvocationLogEntry
import play.api.libs.json.JsValue
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

case class LogEntryData(
                         paramValues: JsValue,
                         context: String,
                         userIdForContext: Option[String],
                         ellipsisUserId: Option[String],
                         timestamp: OffsetDateTime,
                         originalEventType: Option[String]
                       )

object LogEntryData {
  def forEntry(entry: InvocationLogEntry, dataService: DataService)(implicit ec: ExecutionContext): Future[LogEntryData] = {
    val eventualMaybeEllipsisUser = entry.maybeUserIdForContext.map { userIdForContext =>
      dataService.linkedAccounts.find(LoginInfo(entry.context, userIdForContext), entry.behaviorVersion.team.id).map { maybeAcc =>
        maybeAcc.map(_.user)
      }
    }.getOrElse(Future.successful(None))
    eventualMaybeEllipsisUser.map { maybeUser =>
      LogEntryData(
        entry.paramValues,
        entry.context,
        entry.maybeUserIdForContext,
        maybeUser.map(_.id),
        entry.createdAt,
        entry.maybeOriginalEventType.map(_.toString)
      )
    }
  }
}
