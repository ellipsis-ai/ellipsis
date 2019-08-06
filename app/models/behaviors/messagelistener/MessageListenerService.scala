package models.behaviors.messagelistener

import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.events.MessageEvent
import models.team.Team

import scala.concurrent.Future

trait MessageListenerService {

  def createFor(
                 behavior: Behavior,
                 arguments: Map[String, String],
                 user: User,
                 team: Team,
                 medium: String,
                 channel: String,
                 maybeThreadId: Option[String],
                 isForCopilot: Boolean
               ): Future[MessageListener]

  def allFor(
              event: MessageEvent,
              maybeTeam: Option[Team],
              maybeChannel: Option[String],
              context: String
            ): Future[Seq[MessageListener]]

  def allForUser(user: User): Future[Seq[MessageListener]]

}
