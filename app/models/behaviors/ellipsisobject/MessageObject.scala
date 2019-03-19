package models.behaviors.ellipsisobject

import akka.actor.ActorSystem
import json.UserData
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import services.DefaultServices
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

case class MessageObject(
                    text: String,
                    id: Option[String],
                    channel: Option[Channel],
                    thread: Option[String],
                    usersMentioned: Set[UserData],
                    permalink: Option[String],
                    reactionAdded: Option[String]
                  )

object MessageObject {

  def buildForAction(
                      event: Event,
                      maybeConversation: Option[Conversation],
                      services: DefaultServices
                    )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[MessageObject] = {
    for {
      maybePermalink <- DBIO.from(event.maybePermalinkFor(services))
      userDataList <- event.messageUserDataListAction(maybeConversation, services)
      maybeChannelData <- event.eventContext.maybeChannelDataForAction(services)
    } yield {
      MessageObject(
        event.messageText,
        event.maybeMessageId,
        maybeChannelData,
        event.maybeThreadId,
        userDataList,
        maybePermalink,
        event.maybeReactionAdded
      )
    }
  }

}
