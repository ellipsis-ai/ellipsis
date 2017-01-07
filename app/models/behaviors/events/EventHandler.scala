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
import services.slack.MessageEvent

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
    for {
      maybeTeam <- dataService.teams.find(event.teamId)
      responses <- BehaviorResponse.allFor(event, maybeTeam, None, lambdaService, dataService, cache, ws, configuration)
      results <- Future.sequence(responses.map(_.result)).flatMap { r =>
        if (r.isEmpty && event.isResponseExpected) {
          event.noExactMatchResult(dataService, lambdaService).map { noMatchResult =>
            Seq(noMatchResult)
          }
        } else {
          Future.successful(r)
        }
      }
    } yield results
  }

  def interruptOngoingConversationsFor(event: MessageEvent): Future[Unit] = {
    event.allOngoingConversations(dataService).flatMap { ongoing =>
      Future.sequence(ongoing.map { ea =>
        val cancelMessage =
          s""":exclamation: I've been asked to interrupt this conversation for something super duper important*.
             |
             |>You can start it again by saying `${ea.trigger.pattern}`
             |>*may not actually be super duper important""".stripMargin
        cancelConversationResult(ea, cancelMessage).map { result =>
          result.sendIn(event, None, None)
        }
      })
    }.map(_ => {})
  }

  def cancelConversationResult(conversation: Conversation, withMessage: String): Future[BotResult] = {
    conversation.cancel(dataService).map { _ =>
      SimpleTextResult(withMessage, forcePrivateResponse = false)
    }
  }

  def isCancelConversationMessage(text: String): Boolean = {
    Seq("…stop", "…cancel", "...stop", "...cancel").contains(text)
  }

  def handleInConversation(conversation: Conversation, event: MessageEvent): Future[BotResult] = {
    if (isCancelConversationMessage(event.fullMessageText)) {
      cancelConversationResult(conversation, s"OK, I'll stop talking about `${conversation.trigger.pattern}`")
    } else {
      conversation.resultFor(event, lambdaService, dataService, cache, ws, configuration)
    }
  }

  def handle(event: MessageEvent, maybeConversation: Option[Conversation]): Future[Seq[BotResult]] = {
    maybeConversation.map { conversation =>
      handleInConversation(conversation, event).map(Seq(_))
    }.getOrElse {
      BuiltinBehavior.maybeFrom(event, lambdaService, dataService).map { builtin =>
        builtin.result.map(Seq(_))
      }.getOrElse {
        startInvokeConversationFor(event)
      }
    }
  }
}
