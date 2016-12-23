package models.behaviors.conversations

import models.accounts.user.User
import models.behaviors.{BotResult, SimpleTextResult}
import models.behaviors.conversations.conversation.Conversation
import play.api.Configuration
import play.api.cache.CacheApi
import services.DataService
import services.slack.NewMessageEvent

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class UserEnvVarCollectionState(
                                      missingEnvVarNames: Seq[String],
                                      event: NewMessageEvent,
                                      dataService: DataService,
                                      cache: CacheApi,
                                      configuration: Configuration
                                    ) extends CollectionState {

  val name = InvokeBehaviorConversation.COLLECT_USER_ENV_VARS_STATE

  val sortedEnvVars = missingEnvVarNames.sorted

  def maybeNextToCollect: Future[Option[String]] = {
    Future.successful(sortedEnvVars.headOption)
  }

  def isCompleteIn(conversation: Conversation): Future[Boolean] = maybeNextToCollect.map(_.isEmpty)

  def collectValueFrom(conversation: InvokeBehaviorConversation): Future[Conversation] = {
    for {
      maybeNextToCollect <- maybeNextToCollect
      user <- event.ensureUser(dataService)
      updatedConversation <- maybeNextToCollect.map { envVarName =>
        dataService.userEnvironmentVariables.ensureFor(envVarName, Some(event.relevantMessageText), user).map(_ => conversation)
      }.getOrElse(Future.successful(conversation))
      updatedConversation <- updatedConversation.updateToNextState(event, cache, dataService, configuration)
    } yield updatedConversation
  }

  def promptResultFor(conversation: Conversation): Future[BotResult] = {
    maybeNextToCollect.map { maybeNextToCollect =>
      val prompt = maybeNextToCollect.map { envVarName =>
        s"To run this skill, I first need a value for $envVarName. This is specific to you and I'll only ask for it once"
      }.getOrElse {
        "All done!"
      }
      SimpleTextResult(prompt, forcePrivateResponse = true)
    }
  }

}

object UserEnvVarCollectionState {

  def from(
            user: User,
            conversation: Conversation,
            event: NewMessageEvent,
            dataService: DataService,
            cache: CacheApi,
            configuration: Configuration
          ): Future[UserEnvVarCollectionState] = {
    dataService.userEnvironmentVariables.missingFor(user, conversation.behaviorVersion, dataService).map { missing =>
      UserEnvVarCollectionState(missing, event, dataService, cache, configuration)
    }
  }

}
