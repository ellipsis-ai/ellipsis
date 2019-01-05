package models.behaviors.ellipsisobject

import akka.actor.ActorSystem
import json.UserData
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import play.api.libs.json._
import services.DefaultServices
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

case class DeprecatedMessageInfo(
                        text: String,
                        medium: String,
                        mediumDescription: String,
                        channel: Option[String],
                        thread: Option[String],
                        userId: String,
                        details: JsObject,
                        usersMentioned: Set[UserData],
                        permalink: Option[String],
                        reactionAdded: Option[String]
                      )

object DeprecatedMessageInfo {

  def buildForAction(
                event: Event,
                maybeConversation: Option[Conversation],
                services: DefaultServices
              )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[DeprecatedMessageInfo] = {
    for {
      details <- DBIO.from(event.detailsFor(services))
      maybePermalink <- DBIO.from(event.maybePermalinkFor(services))
      userDataList <- event.messageUserDataListAction(maybeConversation, services)
    } yield {
      DeprecatedMessageInfo(
        event.messageText,
        event.eventContext.name,
        event.eventContext.description,
        event.maybeChannel,
        event.maybeThreadId,
        event.eventContext.userIdForContext,
        details,
        userDataList,
        maybePermalink,
        event.maybeReactionAdded
      )
    }
  }

}
