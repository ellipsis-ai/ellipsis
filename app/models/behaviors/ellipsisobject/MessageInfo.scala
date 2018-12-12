package models.behaviors.ellipsisobject

import akka.actor.ActorSystem
import json.UserData
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import play.api.libs.json._
import services.DefaultServices

import scala.concurrent.{ExecutionContext, Future}

case class MessageInfo(
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

object MessageInfo {

  def buildFor(
                event: Event,
                maybeConversation: Option[Conversation],
                services: DefaultServices
              )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[MessageInfo] = {
    for {
      details <- event.detailsFor(services)
      maybePermalink <- event.maybePermalinkFor(services)
      userDataList <- event.messageUserDataList(maybeConversation, services)
    } yield {
      MessageInfo(
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
