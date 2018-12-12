package models.behaviors.ellipsisobject

import akka.actor.ActorSystem
import json.UserData
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import play.api.libs.json._
import services.DefaultServices
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

case class Message(
                    text: String,
                    channel: Option[String],
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
    } yield {
      Message(
        event.messageText,
        event.maybeChannel,
        event.maybeThreadId,
        userDataList,
        maybePermalink,
        event.maybeReactionAdded
      )
    }
  }

}
