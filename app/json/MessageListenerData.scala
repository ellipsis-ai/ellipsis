package json

import java.time.OffsetDateTime

import models.accounts.user.{User, UserTeamAccess}
import models.behaviors.messagelistener.MessageListener
import play.api.libs.json.{JsValue, Json}
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

case class MessageListenerData(
                                id: String,
                                action: Option[BehaviorVersionData],
                                arguments: JsValue,
                                medium: String,
                                channel: String,
                                maybeThreadId: Option[String],
                                user: UserData,
                                createdAt: OffsetDateTime
                              )

object MessageListenerData {

  def from(listener: MessageListener, teamAccess: UserTeamAccess, dataService: DataService)(implicit ec: ExecutionContext): Future[MessageListenerData] = {
    for {
      maybeBehaviorVersion <- dataService.behaviors.maybeCurrentVersionFor(listener.behavior)
      maybeBehaviorVersionData <- BehaviorVersionData.maybeFor(listener.behavior.id,
        teamAccess.user,
        dataService: DataService,
        maybeBehaviorVersion.map(_.groupVersion),
        None)
      userData <- dataService.users.userDataFor(listener.user, teamAccess.maybeTargetTeam.getOrElse(teamAccess.loggedInTeam))
    } yield {
      MessageListenerData(
        listener.id,
        maybeBehaviorVersionData,
        Json.toJson(listener.arguments),
        listener.medium,
        listener.channel,
        listener.maybeThreadId,
        userData,
        listener.createdAt
      )
    }
  }

}
