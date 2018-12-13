package models.behaviors.ellipsisobject

import akka.actor.ActorSystem
import json.UserData
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import services.DefaultServices
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

case class Message(
                    text: String,
                    channel: Option[Channel],
                    thread: Option[String],
                    usersMentioned: Set[UserData],
                    permalink: Option[String],
                    reactionAdded: Option[String]
                  )

object Message {

  def buildForAction(
                      event: Event,
                      maybeConversation: Option[Conversation],
                      services: DefaultServices
                    )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Message] = {
    for {
      maybePermalink <- DBIO.from(event.maybePermalinkFor(services))
      userDataList <- event.messageUserDataListAction(maybeConversation, services)
      maybeChannelData <- event.eventContext.maybeChannelDataForAction(services)
    } yield {
      Message(
        event.messageText,
        maybeChannelData,
        event.maybeThreadId,
        userDataList,
        maybePermalink,
        event.maybeReactionAdded
      )
    }
  }

}
