package models.behaviors.events

import javax.inject._

import akka.actor.ActorSystem
import models.behaviors.builtins.BuiltinBehavior
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.{BehaviorResponse, BotResult, SimpleTextResult, TextWithActionsResult}
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSClient
import services.{AWSLambdaService, DataService}
import utils.Color

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

@Singleton
class EventHandler @Inject() (
                               lambdaService: AWSLambdaService,
                               dataService: DataService,
                               cache: CacheApi,
                               messages: MessagesApi,
                               ws: WSClient,
                               configuration: Configuration,
                               implicit val actorSystem: ActorSystem
                               ) {

  def startInvokeConversationFor(event: Event): Future[Seq[BotResult]] = {
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

  def interruptOngoingConversationsFor(event: Event): Future[Boolean] = {
    event.allOngoingConversations(dataService).flatMap { ongoing =>
      Future.sequence(ongoing.map { ea =>
        dataService.conversations.background(ea)
      })
    }.map(interruptionResults => interruptionResults.nonEmpty)
  }

  def cancelConversationResult(event: Event, conversation: Conversation, withMessage: String): Future[BotResult] = {
    conversation.cancel(dataService).map { _ =>
      SimpleTextResult(event, withMessage, forcePrivateResponse = false)
    }
  }

  def isCancelConversationMessage(event: Event): Boolean = {
    val text = event.messageText
    val mentionedBot = event.includesBotMention
    val shortcutPlusKeyword = "^(\\.\\.\\.|…)(stop|cancel|skip)".r.findFirstIn(text).isDefined
    val mentionedPlusKeyword = mentionedBot && "^<@.+?>:?\\s+(stop|cancel|skip)$".r.findFirstIn(text).isDefined
    /*
    One could imagine allowing the stop words to work with no prefix in a DM with Ellipsis, but since such words
    could also be valid answers, disabling this for now.

    val isDMPlusKeyword = mentionedBot && "^(stop|cancel|skip)$".r.findFirstIn(text).isDefined */
    shortcutPlusKeyword || mentionedPlusKeyword /* || isDMPlusKeyword */
  }

  def handleInConversation(conversation: Conversation, event: Event): Future[BotResult] = {
    if (isCancelConversationMessage(event)) {
      cancelConversationResult(event, conversation, s"OK, I’ll stop asking about that.")
    } else {
      if (conversation.isStale) {
        val key = conversation.pendingEventKey
        cache.set(key, event, 5.minutes)
        val actions = Seq(
          SlackMessageAction("confirm_continue_conversation", "Yes", conversation.id),
          SlackMessageAction("dont_continue_conversation", "No", conversation.id)
        )
        val prompt = "It's been a while since I asked you the question above. Just so I'm sure, is this an answer to it?"
        val attachment = SlackMessageActions("should_continue_conversation", actions, None, Some(Color.PINK))
        Future.successful(TextWithActionsResult(event, prompt, forcePrivateResponse = false, attachment))
      } else {
        conversation.resultFor(event, lambdaService, dataService, cache, ws, configuration)
      }
    }
  }

  def handle(event: Event, maybeConversation: Option[Conversation]): Future[Seq[BotResult]] = {
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
