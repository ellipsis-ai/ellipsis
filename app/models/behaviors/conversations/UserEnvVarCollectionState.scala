package models.behaviors.conversations

import models.accounts.user.User
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import models.behaviors.{BotResult, SimpleTextResult}
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class UserEnvVarCollectionState(
                                      missingEnvVarNames: Seq[String],
                                      event: Event,
                                      services: ConversationServices
                                    ) extends CollectionState {

  lazy val dataService = services.dataService

  val name = InvokeBehaviorConversation.COLLECT_USER_ENV_VARS_STATE

  val sortedEnvVars = missingEnvVarNames.sorted

  def maybeNextToCollectAction: DBIO[Option[String]] = {
    DBIO.successful(sortedEnvVars.headOption)
  }

  def maybeNextToCollect: Future[Option[String]] = {
    dataService.run(maybeNextToCollectAction)
  }

  def isCompleteIn(conversation: Conversation): Future[Boolean] = maybeNextToCollect.map(_.isEmpty)

  def collectValueFrom(conversation: InvokeBehaviorConversation): Future[Conversation] = {
    for {
      maybeNextToCollect <- maybeNextToCollect
      user <- event.ensureUser(dataService)
      updatedConversation <- maybeNextToCollect.map { envVarName =>
        dataService.userEnvironmentVariables.ensureFor(envVarName, Some(event.relevantMessageText), user).map(_ => conversation)
      }.getOrElse(Future.successful(conversation))
      updatedConversation <- updatedConversation.updateToNextState(event, services)
    } yield updatedConversation
  }

  def promptResultForAction(conversation: Conversation, isReminding: Boolean): DBIO[BotResult] = {
    maybeNextToCollectAction.map { maybeNextToCollect =>
      val prompt = maybeNextToCollect.map { envVarName =>
        s"To run this skill, I first need a value for $envVarName. This is specific to you and I'll only ask for it once"
      }.getOrElse {
        "All done!"
      }
      SimpleTextResult(event, Some(conversation), prompt, forcePrivateResponse = true)
    }
  }

}

object UserEnvVarCollectionState {

  def fromAction(
                  user: User,
                  conversation: Conversation,
                  event: Event,
                  services: ConversationServices
                ): DBIO[UserEnvVarCollectionState] = {
    val dataService = services.dataService
    dataService.userEnvironmentVariables.missingForAction(user, conversation.behaviorVersion, dataService).map { missing =>
      UserEnvVarCollectionState(missing, event, services)
    }
  }

}
