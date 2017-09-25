package models.behaviors.conversations

import models.accounts.user.User
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import models.behaviors.{BotResult, SimpleTextResult}
import services.DefaultServices
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

case class UserEnvVarCollectionState(
                                      missingEnvVarNames: Set[String],
                                      event: Event,
                                      services: DefaultServices
                                    ) extends CollectionState {

  lazy val dataService = services.dataService

  val name = InvokeBehaviorConversation.COLLECT_USER_ENV_VARS_STATE

  val sortedEnvVars = missingEnvVarNames.toSeq.sorted

  def maybeNextToCollectAction: DBIO[Option[String]] = {
    DBIO.successful(sortedEnvVars.headOption)
  }

  def maybeNextToCollect: Future[Option[String]] = {
    dataService.run(maybeNextToCollectAction)
  }

  def isCompleteIn(conversation: Conversation)(implicit ec: ExecutionContext): Future[Boolean] = maybeNextToCollect.map(_.isEmpty)

  def collectValueFrom(conversation: InvokeBehaviorConversation)(implicit ec: ExecutionContext): Future[Conversation] = {
    for {
      maybeNextToCollect <- maybeNextToCollect
      user <- event.ensureUser(dataService)
      updatedConversation <- maybeNextToCollect.map { envVarName =>
        dataService.userEnvironmentVariables.ensureFor(envVarName, Some(event.relevantMessageText), user).map(_ => conversation)
      }.getOrElse(Future.successful(conversation))
      updatedConversation <- updatedConversation.updateToNextState(event, services)
    } yield updatedConversation
  }

  def promptResultForAction(conversation: Conversation, isReminding: Boolean)(implicit ec: ExecutionContext): DBIO[BotResult] = {
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
                  services: DefaultServices
                )(implicit ec: ExecutionContext): DBIO[UserEnvVarCollectionState] = {
    val dataService = services.dataService
    dataService.userEnvironmentVariables.missingForAction(user, conversation.behaviorVersion, dataService).map { missing =>
      UserEnvVarCollectionState(missing, event, services)
    }
  }

}
