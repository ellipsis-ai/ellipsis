package models.behaviors.messagelistener

import models.accounts.user.User
import models.behaviors.ActionArg
import models.behaviors.behavior.Behavior
import models.behaviors.events.MessageEvent
import models.team.Team

import scala.concurrent.Future

trait MessageListenerService {

  def createFor(
                 behavior: Behavior,
                 arguments: Seq[ActionArg],
                 user: User,
                 team: Team,
                 medium: String,
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
