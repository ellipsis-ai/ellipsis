package models.bots.events

import javax.inject._

import models.bots.{BehaviorResponse, BehaviorResult, NoResponseResult, SimpleTextResult}
import models.bots.builtins.BuiltinBehavior
import models.bots.conversations.conversation.Conversation
import play.api.i18n.MessagesApi
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class EventHandler @Inject() (
                               lambdaService: AWSLambdaService,
                               dataService: DataService,
                               messages: MessagesApi
                               ) {

  def startInvokeConversationFor(event: MessageEvent): Future[BehaviorResult] = {
    val context = event.context
    for {
      maybeTeam <- dataService.teams.find(context.teamId)
      maybeResponse <- BehaviorResponse.chooseFor(event, maybeTeam, None, dataService)
      result <- maybeResponse.map { response =>
        response.result(lambdaService, dataService)
      }.getOrElse {
        val result = if (context.isResponseExpected) {
          SimpleTextResult(context.iDontKnowHowToRespondMessageFor(lambdaService))
        } else {
          NoResponseResult(None)
        }
        Future.successful(result)
      }
    } yield result
  }

  def handleInConversation(conversation: Conversation, event: MessageEvent): Future[BehaviorResult] = {
    conversation.resultFor(event, lambdaService, dataService)
  }

  def handle(event: Event): Future[BehaviorResult] = {
    event match {
      case messageEvent: MessageEvent => {
        for {
          maybeConversation <- event.context.maybeOngoingConversation(dataService)
          result <- maybeConversation.map { conversation =>
            handleInConversation(conversation, messageEvent)
          }.getOrElse {
            BuiltinBehavior.maybeFrom(messageEvent.context, lambdaService, dataService).map { builtin =>
              builtin.result
            }.getOrElse {
              startInvokeConversationFor(messageEvent)
            }
          }
        } yield result
      }
      case _ => Future.successful(NoResponseResult(None))
    }

  }
}
