package models.behaviors.conversations

import models.accounts.linkedsimpletoken.LinkedSimpleToken
import models.accounts.simpletokenapi.SimpleTokenApi
import models.accounts.user.User
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

  val name = InvokeBehaviorConversation.COLLECT_SIMPLE_TOKENS_STATE

  def maybeNextToCollectAction: DBIO[Option[SimpleTokenApi]] = {
    DBIO.successful(missingTokenApis.headOption)
  }

  def maybeNextToCollect: Future[Option[SimpleTokenApi]] = {
    dataService.run(maybeNextToCollectAction)
  }

  def isCompleteIn(conversation: Conversation)(implicit ec: ExecutionContext): Future[Boolean] = maybeNextToCollect.map(_.isEmpty)

  def collectValueFrom(conversation: InvokeBehaviorConversation)(implicit ec: ExecutionContext): Future[Conversation] = {
    for {
      maybeNextToCollect <- maybeNextToCollect
      user <- event.ensureUser(dataService)
      updatedConversation <- maybeNextToCollect.map { api =>
        val token = event.relevantMessageText.trim
        dataService.linkedSimpleTokens.save(LinkedSimpleToken(token, user.id, api)).map(_ => conversation)
      }.getOrElse(Future.successful(conversation))
      updatedConversation <- updatedConversation.updateToNextState(event, services)
    } yield updatedConversation
  }

  def promptResultForAction(conversation: Conversation, isReminding: Boolean)(implicit ec: ExecutionContext): DBIO[BotResult] = {
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
      SimpleTextResult(event, Some(conversation), prompt, forcePrivateResponse = true)
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
