package models.behaviors.messagelistener

import java.time.OffsetDateTime

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import javax.inject.Inject
import models.IDs
import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.events.MessageEvent
import models.team.Team
import play.api.libs.json.JsValue
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
                              createdAt: OffsetDateTime
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

  def * =
    (id, behaviorId, arguments, medium, channel, maybeThreadId, userId, isForCopilot, isEnabled, createdAt) <> ((RawMessageListener.apply _).tupled, RawMessageListener.unapply _)
}

class MessageListenerServiceImpl @Inject() (
                                            dataServiceProvider: Provider[DataService],
                                            implicit val ec: ExecutionContext
                                          ) extends MessageListenerService {

  def dataService = dataServiceProvider.get

  import MessageListenerQueries._

  def createForAction(
                       behavior: Behavior,
                       arguments: Map[String, String],
                       user: User,
                       team: Team,
                       medium: String,
                       channel: String,
                       maybeThreadId: Option[String],
                       isForCopilot: Boolean
                     ): DBIO[MessageListener] = {
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
      OffsetDateTime.now
    )
    (all += newInstance.toRaw).map(_ => newInstance)
  }

  def createFor(
                 behavior: Behavior,
                 arguments: Map[String, String],
                 user: User,
                 team: Team,
                 medium: String,
                 channel: String,
                 maybeThreadId: Option[String],
                 isForCopilot: Boolean
               ): Future[MessageListener] = {
    dataService.run(createForAction(behavior, arguments, user, team, medium, channel, maybeThreadId, isForCopilot))
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

  def allForUserAction(user: User): DBIO[Seq[MessageListener]] = {
    allForUserQuery(user.id).result.map { r =>
      r.map(tuple2Listener)
    }
  }

  def allForUser(user: User): Future[Seq[MessageListener]] = {
    dataService.run(allForUserAction(user))
  }
}
