package models.bots.events

import javax.inject._

import models.bots.{BehaviorResponse, BehaviorResult, NoResponseResult, SimpleTextResult}
import models.bots.builtins.BuiltinBehavior
import models.bots.conversations.Conversation
import models.{Models, Team}
import play.api.i18n.MessagesApi
import services.AWSLambdaService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class EventHandler @Inject() (
                               lambdaService: AWSLambdaService,
                               models: Models,
                               messages: MessagesApi
                               ) {

  def startInvokeConversationFor(event: MessageEvent): DBIO[BehaviorResult] = {
    val context = event.context
    for {
      maybeTeam <- Team.find(context.teamId)
      maybeResponse <- BehaviorResponse.chooseFor(event, maybeTeam, None)
      result <- maybeResponse.map { response =>
        response.result(lambdaService)
      }.getOrElse {
        val result = if (context.isResponseExpected) {
          SimpleTextResult(context.iDontKnowHowToRespondMessageFor(lambdaService))
        } else {
          NoResponseResult(None)
        }
        DBIO.successful(result)
      }
    } yield result
  }

  def handleInConversation(conversation: Conversation, event: MessageEvent): DBIO[BehaviorResult] = {
    conversation.resultFor(event, lambdaService)
  }

  def handle(event: Event): Future[BehaviorResult] = {
    event match {
      case messageEvent: MessageEvent => {
        val action = for {
          maybeConversation <- event.context.maybeOngoingConversation
          result <- maybeConversation.map { conversation =>
            handleInConversation(conversation, messageEvent)
          }.getOrElse {
            BuiltinBehavior.maybeFrom(messageEvent.context, lambdaService).map { builtin =>
              builtin.result
            }.getOrElse {
              startInvokeConversationFor(messageEvent)
            }
          }
        } yield result
        models.run(action)
      }
      case _ => Future.successful(NoResponseResult(None))
    }

  }
}
