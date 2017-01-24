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

  def interruptOngoingConversationsFor(event: MessageEvent): Future[Boolean] = {
    event.allOngoingConversations(dataService).flatMap { ongoing =>
      Future.sequence(ongoing.map { ea =>
        val cancelMessage =
          s"""_(skipping question for now)_
             |
             |:wave: Hey.
             |
             |You haven’t answered my question above yet. When you’re ready to answer, just say `${ea.trigger.pattern}`.
             |""".stripMargin
        cancelConversationResult(event, ea, cancelMessage).flatMap { result =>
          result.sendIn(None, None).map(_ => result)
        }
      })
    }.map(interruptionResults => interruptionResults.nonEmpty)
  }

  def cancelConversationResult(event: MessageEvent, conversation: Conversation, withMessage: String): Future[BotResult] = {
    conversation.cancel(dataService).map { _ =>
      SimpleTextResult(event, withMessage, forcePrivateResponse = false)
    }
  }

  def isCancelConversationMessage(event: MessageEvent): Boolean = {
    val text = event.fullMessageText
    val mentionedBot = event.includesBotMention
    val shortcutPlusKeyword = "^(\\.\\.\\.|…)(stop|cancel|skip)".r.findFirstIn(text).isDefined
    val mentionedPlusKeyword = mentionedBot && "^<@.+?>:?\\s+(stop|cancel|skip)$".r.findFirstIn(text).isDefined
    /*
    One could imagine allowing the stop words to work with no prefix in a DM with Ellipsis, but since such words
    could also be valid answers, disabling this for now.

    val isDMPlusKeyword = mentionedBot && "^(stop|cancel|skip)$".r.findFirstIn(text).isDefined */
    shortcutPlusKeyword || mentionedPlusKeyword /* || isDMPlusKeyword */
  }

  def handleInConversation(conversation: Conversation, event: MessageEvent): Future[BotResult] = {
    if (isCancelConversationMessage(event)) {
      cancelConversationResult(event, conversation, s"OK, I’ll stop asking about that.")
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
