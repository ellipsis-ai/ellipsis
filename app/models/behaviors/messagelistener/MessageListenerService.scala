package models.behaviors.messagelistener

import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.events.MessageEvent
import models.behaviors.input.Input
import models.team.Team

import scala.concurrent.Future

trait MessageListenerService {

  def createFor(
                 behavior: Behavior,
                 messageInput: Input,
                 arguments: Map[String, String],
                 user: User,
                 team: Team,
                 channel: String,
                 maybeThreadId: Option[String]
               ): Future[MessageListener]

  def allFor(
              event: MessageEvent,
              maybeTeam: Option[Team],
              maybeChannel: Option[String],
              context: String
            ): Future[Seq[MessageListener]]

}
