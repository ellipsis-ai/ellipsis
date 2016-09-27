package models.behaviors.events

import javax.inject._

import models.behaviors.{BehaviorResponse, BotResult, NoResponseResult, SimpleTextResult}
import models.behaviors.builtins.BuiltinBehavior
import models.behaviors.conversations.conversation.Conversation
import play.api.cache.CacheApi
import play.api.i18n.MessagesApi
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class EventHandler @Inject() (
                               lambdaService: AWSLambdaService,
                               dataService: DataService,
                               cache: CacheApi,
                               messages: MessagesApi
                               ) {

  def startInvokeConversationFor(event: MessageEvent): Future[BotResult] = {
    val context = event.context
    for {
      maybeTeam <- dataService.teams.find(context.teamId)
      maybeResponse <- BehaviorResponse.chooseFor(event, maybeTeam, None, lambdaService, dataService, cache)
      result <- maybeResponse.map { response =>
        response.result
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

  def handleInConversation(conversation: Conversation, event: MessageEvent): Future[BotResult] = {
    conversation.resultFor(event, lambdaService, dataService, cache)
  }

  def handle(event: Event): Future[BotResult] = {
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
