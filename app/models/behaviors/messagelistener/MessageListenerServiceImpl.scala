package models.behaviors.messagelistener

import java.time.OffsetDateTime

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import javax.inject.Inject
import models.IDs
import models.accounts.user.{User, UserTeamAccess}
import models.behaviors.behavior.Behavior
import models.behaviors.events.MessageEvent
import models.team.Team
import play.api.libs.json.{JsValue, Json}
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

case class RawMessageListener(
                               id: String,
                               behaviorId: String,
                               arguments: JsValue,
                               medium: String,
                               channel: String,
                               maybeThreadId: Option[String],
                               userId: String,
                               isForCopilot: Boolean,
                               isEnabled: Boolean,
                               createdAt: OffsetDateTime,
                               maybeLastCopilotActivityAt: Option[OffsetDateTime]
                          )

class MessageListenersTable(tag: Tag) extends Table[RawMessageListener](tag, "message_listeners") {

  def id = column[String]("id", O.PrimaryKey)
  def behaviorId = column[String]("behavior_id")
  def arguments = column[JsValue]("arguments")
  def medium = column[String]("medium")
  def channel = column[String]("channel")
  def maybeThreadId = column[Option[String]]("thread")
  def userId = column[String]("user_id")
  def isForCopilot = column[Boolean]("is_for_copilot")
  def isEnabled = column[Boolean]("is_enabled")
  def createdAt = column[OffsetDateTime]("created_at")
  def maybeLastCopilotActivityAt = column[Option[OffsetDateTime]]("last_copilot_activity_at")

  def * =
    (id, behaviorId, arguments, medium, channel, maybeThreadId, userId, isForCopilot, isEnabled, createdAt, maybeLastCopilotActivityAt) <> ((RawMessageListener.apply _).tupled, RawMessageListener.unapply _)
}

class MessageListenerServiceImpl @Inject() (
                                            dataServiceProvider: Provider[DataService],
                                            implicit val ec: ExecutionContext
                                          ) extends MessageListenerService {

  def dataService = dataServiceProvider.get

  import MessageListenerQueries._

  def findWithoutAccessCheck(id: String): Future[Option[MessageListener]] = {
    val action = findWithoutAccessCheckQuery(id).result.headOption.map { r =>
      r.map(tuple2Listener)
    }
    dataService.run(action)
  }

  def find(id: String, user: User): Future[Option[MessageListener]] = {
    for {
      maybeListener <- dataService.messageListeners.findWithoutAccessCheck(id)
      teamAccess <- dataService.users.teamAccessFor(user, maybeListener.map(_.behavior.team.id))
    } yield {
      if (teamAccess.isAdminUser) {
        maybeListener
      } else {
        maybeListener.filter(_.behavior.team.id == teamAccess.loggedInTeam.id)
      }
    }
  }

  def ensureForAction(
                       behavior: Behavior,
                       arguments: Map[String, String],
                       user: User,
                       team: Team,
                       medium: String,
                       channel: String,
                       maybeThreadId: Option[String],
                       isForCopilot: Boolean
                     ): DBIO[MessageListener] = {
    findForEnsureQuery(behavior.id, Json.toJson(arguments), user.id, medium, channel, maybeThreadId, isForCopilot).result.flatMap { r =>
      r.headOption.map(tuple2Listener).map { existing =>
        all.map(_.isEnabled).update(true).map { _ =>
          existing.copy(isEnabled = true)
        }
      }.getOrElse {
        val newInstance = MessageListener(
          IDs.next,
          behavior,
          arguments,
          medium,
          channel,
          maybeThreadId,
          user,
          isForCopilot,
          isEnabled = true,
          OffsetDateTime.now,
          maybeLastCopilotActivityAt = None
        )
        (all += newInstance.toRaw).map(_ => newInstance)
      }
    }
  }

  def noteCopilotActivityAction(listener: MessageListener): DBIO[Unit] = {
    if (listener.isForCopilot) {
      noteCopilotActivityQuery(listener.id).update(true, Some(OffsetDateTime.now)).map(_ => {})
    } else {
      DBIO.successful({})
    }
  }

  def noteCopilotActivity(listener: MessageListener): Future[Unit] = {
    dataService.run(noteCopilotActivityAction(listener))
  }

  def disableIdleListeners: Future[Unit] = {
    val action = idleCopilotListenersQuery(OffsetDateTime.now.minusHours(1)).update(false).map(_ => {})
    dataService.run(action)
  }

  def ensureFor(
                 behavior: Behavior,
                 arguments: Map[String, String],
                 user: User,
                 team: Team,
                 medium: String,
                 channel: String,
                 maybeThreadId: Option[String],
                 isForCopilot: Boolean
               ): Future[MessageListener] = {
    dataService.run(ensureForAction(behavior, arguments, user, team, medium, channel, maybeThreadId, isForCopilot))
  }

  def allForAction(
                    event: MessageEvent,
                    maybeTeam: Option[Team],
                    maybeChannel: Option[String],
                    context: String
                  ): DBIO[Seq[MessageListener]] = {
    (for {
      team <- maybeTeam
      channel <- maybeChannel
    } yield {
      allForQuery(team.id, event.eventContext.name, channel, event.maybeThreadId).result.map { r =>
        r.map(tuple2Listener)
      }
    }).getOrElse(DBIO.successful(Seq()))
  }

  def allFor(
              event: MessageEvent,
              maybeTeam: Option[Team],
              maybeChannel: Option[String],
              context: String
            ): Future[Seq[MessageListener]] = {
    dataService.run(allForAction(event, maybeTeam, maybeChannel, context))
  }

  def disableFor(
                  behavior: Behavior,
                  user: User,
                  medium: String,
                  channel: String,
                  maybeThreadId: Option[String],
                  isForCopilot: Boolean
                ): Future[Int] = {
    dataService.run(isEnabledForUserBehavior(behavior.id, user.id, medium, channel, maybeThreadId, isForCopilot).update(false))
  }

}
