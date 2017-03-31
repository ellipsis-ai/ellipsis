package models.behaviors.conversations

import models.accounts.linkedsimpletoken.LinkedSimpleToken
import models.accounts.simpletokenapi.SimpleTokenApi
import models.accounts.user.User
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import models.behaviors.{BotResult, SimpleTextResult}
import play.api.Configuration
import play.api.cache.CacheApi
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class SimpleTokenCollectionState(
                                       missingTokenApis: Seq[SimpleTokenApi],
                                       event: Event,
                                       dataService: DataService,
                                       cache: CacheApi,
                                       configuration: Configuration
                                    ) extends CollectionState {

  val name = InvokeBehaviorConversation.COLLECT_SIMPLE_TOKENS_STATE

  def maybeNextToCollect: Future[Option[SimpleTokenApi]] = {
    Future.successful(missingTokenApis.headOption)
  }

  def isCompleteIn(conversation: Conversation): Future[Boolean] = maybeNextToCollect.map(_.isEmpty)

  def collectValueFrom(conversation: InvokeBehaviorConversation): Future[Conversation] = {
    for {
      maybeNextToCollect <- maybeNextToCollect
      user <- event.ensureUser(dataService)
      updatedConversation <- maybeNextToCollect.map { api =>
        val token = event.relevantMessageText.trim
        dataService.linkedSimpleTokens.save(LinkedSimpleToken(token, user.id, api)).map(_ => conversation)
      }.getOrElse(Future.successful(conversation))
      updatedConversation <- updatedConversation.updateToNextState(event, cache, dataService, configuration)
    } yield updatedConversation
  }

  def promptResultFor(conversation: Conversation): Future[BotResult] = {
    maybeNextToCollect.map { maybeNextToCollect =>
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
      SimpleTextResult(event, prompt, forcePrivateResponse = true)
    }
  }

}

object SimpleTokenCollectionState {

  def from(
            user: User,
            conversation: Conversation,
            event: Event,
            dataService: DataService,
            cache: CacheApi,
            configuration: Configuration
          ): Future[SimpleTokenCollectionState] = {
    for {
      tokens <- dataService.linkedSimpleTokens.allForUser(user)
      requiredTokenApis <- dataService.requiredSimpleTokenApis.allFor(conversation.behaviorVersion.groupVersion)
    } yield {
      val missing = requiredTokenApis.filterNot { required =>
        tokens.exists(linked => linked.api == required.api)
      }.map(_.api)
      SimpleTokenCollectionState(missing, event, dataService, cache, configuration)
    }
  }

}
