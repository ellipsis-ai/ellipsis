package models.bots

import javax.inject._
import models.{Team, Models}
import models.bots.builtins.BuiltinBehavior
import models.bots.conversations.Conversation
import play.api.i18n.MessagesApi
import services.AWSLambdaService
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import slick.driver.PostgresDriver.api._

@Singleton
class EventHandler @Inject() (
                               lambdaService: AWSLambdaService,
                               models: Models,
                               messages: MessagesApi
                               ) {

  def startInvokeConversationFor(event: Event): DBIO[Unit] = {
    val context = event.context
    for {
      maybeTeam <- Team.find(context.teamId)
      maybeResponse <- BehaviorResponse.chooseFor(event, maybeTeam, None)
      _ <- maybeResponse.map { response =>
        response.run(lambdaService)
      }.getOrElse {
        if (context.isResponseExpected) {
          context.sendIDontKnowHowToRespondMessageFor(lambdaService)
        }
        DBIO.successful(Unit)
      }
    } yield Unit
  }

  def handleInConversation(conversation: Conversation, event: Event): DBIO[Unit] = {
    conversation.replyFor(event, lambdaService)
  }

  def handle(event: Event): Future[Unit] = {
    val action = event.context.maybeOngoingConversation.flatMap { maybeConversation =>
      maybeConversation.map { conversation =>
        handleInConversation(conversation, event)
      }.getOrElse {
        BuiltinBehavior.maybeFrom(event.context, lambdaService).map { builtin =>
          builtin.run
        }.getOrElse {
          startInvokeConversationFor(event)
        }
      }
    }
    models.run(action)
  }
}
