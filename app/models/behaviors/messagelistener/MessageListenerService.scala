package models.behaviors.messagelistener

import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.events.MessageEvent
import models.team.Team

import scala.concurrent.Future

trait MessageListenerService {

  def findWithoutAccessCheck(id: String): Future[Option[MessageListener]]

  def find(id: String, user: User): Future[Option[MessageListener]]

  def ensureFor(
                 behavior: Behavior,
                 arguments: Map[String, String],
                 user: User,
                 team: Team,
                 medium: String,
                 channel: String,
                 maybeThreadId: Option[String],
                 isForCopilot: Boolean
               ): Future[MessageListener]

  def noteCopilotActivity(listener: MessageListener): Future[Unit]

  def disableIdleListeners: Future[Unit]

  def allFor(
              event: MessageEvent,
              maybeTeam: Option[Team],
              maybeChannel: Option[String],
              context: String
            ): Future[Seq[MessageListener]]

  def disableFor(
                  behavior: Behavior,
                  user: User,
                  medium: String,
                  channel: String,
                  maybeThreadId: Option[String],
                  isForCopilot: Boolean
                ): Future[Int]

}
