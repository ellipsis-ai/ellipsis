package models.behaviors.events

import javax.inject._

import models.behaviors.{BehaviorResponse, BotResult, NoResponseResult, SimpleTextResult}
import models.behaviors.builtins.BuiltinBehavior
import models.behaviors.conversations.conversation.Conversation
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSClient
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class EventHandler @Inject() (
                               lambdaService: AWSLambdaService,
                               dataService: DataService,
                               cache: CacheApi,
                               messages: MessagesApi,
                               ws: WSClient,
                               configuration: Configuration
                               ) {

  def startInvokeConversationFor(event: MessageEvent): Future[Seq[BotResult]] = {
    val context = event.context
    for {
      maybeTeam <- dataService.teams.find(context.teamId)
      responses <- BehaviorResponse.allFor(event, maybeTeam, None, lambdaService, dataService, cache, ws, configuration)
      results <- Future.sequence(responses.map(_.result))
    } yield {
      if (results.isEmpty && context.isResponseExpected) {
        Seq(SimpleTextResult(context.iDontKnowHowToRespondMessageFor(lambdaService), forcePrivateResponse = false))
      } else {
        results
      }
    }
  }

  def handleInConversation(conversation: Conversation, event: MessageEvent): Future[BotResult] = {
    conversation.resultFor(event, lambdaService, dataService, cache, ws, configuration)
  }

  def handle(event: Event): Future[Seq[BotResult]] = {
    event match {
      case messageEvent: MessageEvent => {
        for {
          maybeConversation <- event.context.maybeOngoingConversation(dataService)
          results <- maybeConversation.map { conversation =>
            handleInConversation(conversation, messageEvent).map(Seq(_))
          }.getOrElse {
            BuiltinBehavior.maybeFrom(messageEvent.context, lambdaService, dataService).map { builtin =>
              builtin.result.map(Seq(_))
            }.getOrElse {
              startInvokeConversationFor(messageEvent)
            }
          }
        } yield results
      }
      case _ => Future.successful(Seq(NoResponseResult(None)))
    }

  }
}
