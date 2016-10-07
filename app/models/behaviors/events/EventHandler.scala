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

  def startInvokeConversationFor(event: MessageEvent): Future[Seq[BotResult]] = {
    val context = event.context
    for {
      maybeTeam <- dataService.teams.find(context.teamId)
      responses <- BehaviorResponse.allFor(event, maybeTeam, None, lambdaService, dataService, cache)
      results <- Future.sequence(responses.map(_.result))
    } yield {
      if (results.isEmpty && context.isResponseExpected) {
        Seq(SimpleTextResult(context.iDontKnowHowToRespondMessageFor(lambdaService)))
      } else {
        results
      }
    }
  }

  def handleInConversation(conversation: Conversation, event: MessageEvent): Future[BotResult] = {
    conversation.resultFor(event, lambdaService, dataService, cache)
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
