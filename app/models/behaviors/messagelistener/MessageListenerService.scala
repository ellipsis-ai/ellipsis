package models.behaviors.messagelistener

import models.accounts.user.{User, UserTeamAccess}
import models.behaviors.behavior.Behavior
import models.behaviors.events.MessageEvent
import models.team.Team

import scala.concurrent.Future

trait MessageListenerService {

  def find(id: String, teamAccess: UserTeamAccess): Future[Option[MessageListener]]

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

  def disableFor(
                  behavior: Behavior,
                  user: User,
                  medium: String,
                  channel: String,
                  maybeThreadId: Option[String],
                  isForCopilot: Boolean
                ): Future[Int]

}
