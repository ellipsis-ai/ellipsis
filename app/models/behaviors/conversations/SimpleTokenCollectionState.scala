package models.behaviors.conversations

import akka.actor.ActorSystem
import models.accounts.linkedsimpletoken.LinkedSimpleToken
import models.accounts.simpletokenapi.SimpleTokenApi
import models.accounts.user.User
import models.behaviors.behaviorversion.Private
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import models.behaviors.{BotResult, SimpleTextResult}
import services.DefaultServices
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

case class SimpleTokenCollectionState(
                                       missingTokenApis: Seq[SimpleTokenApi],
                                       event: Event,
                                       services: DefaultServices
                                    ) extends CollectionState {

  lazy val dataService = services.dataService

  val name = Conversation.COLLECT_SIMPLE_TOKENS_STATE

  def maybeNextToCollectAction: DBIO[Option[SimpleTokenApi]] = {
    DBIO.successful(missingTokenApis.headOption)
  }

  def maybeNextToCollect: Future[Option[SimpleTokenApi]] = {
    dataService.run(maybeNextToCollectAction)
  }

  def isCompleteInAction(conversation: Conversation)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Boolean] = {
    maybeNextToCollectAction.map(_.isEmpty)
  }

  def collectValueFromAction(conversation: InvokeBehaviorConversation)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Conversation] = {
    for {
      maybeNextToCollect <- maybeNextToCollectAction
      user <- event.ensureUserAction(dataService)
      updatedConversation <- maybeNextToCollect.map { api =>
        val token = event.relevantMessageText.trim
        dataService.linkedSimpleTokens.saveAction(LinkedSimpleToken(token, user.id, api)).map(_ => conversation)
      }.getOrElse(DBIO.successful(conversation))
      updatedConversation <- updatedConversation.updateToNextStateAction(event, services)
    } yield updatedConversation
  }

  def promptResultForAction(conversation: Conversation, isReminding: Boolean)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult] = {
    maybeNextToCollectAction.map { maybeNextToCollect =>
      val prompt = maybeNextToCollect.map { api =>
        s"""
           |To use this skill, you need to provide your ${api.name} API token.
           |
           |You can find it by visiting ${api.maybeTokenUrl.getOrElse("")}.
           |
           |Once you have found it, enter it here or type `...cancel` if you're not ready yet.
           |""".stripMargin
      }.getOrElse {
        "All done!"
      }
      SimpleTextResult(event, Some(conversation), prompt, responseType = Private)
    }
  }

}

object SimpleTokenCollectionState {

  def fromAction(
                  user: User,
                  conversation: Conversation,
                  event: Event,
                  services: DefaultServices
          )(implicit ec: ExecutionContext): DBIO[SimpleTokenCollectionState] = {
    val dataService = services.dataService
    for {
      tokens <- dataService.linkedSimpleTokens.allForUserAction(user)
      requiredTokenApis <- dataService.requiredSimpleTokenApis.allForAction(conversation.behaviorVersion.groupVersion)
    } yield {
      val missing = requiredTokenApis.filterNot { required =>
        tokens.exists(linked => linked.api == required.api)
      }.map(_.api)
      SimpleTokenCollectionState(missing, event, services)
    }
  }

}
