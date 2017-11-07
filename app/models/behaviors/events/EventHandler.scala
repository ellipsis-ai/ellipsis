package models.behaviors.events

import javax.inject._

import models.behaviors.builtins.BuiltinBehavior
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.SlackMessageActionConstants._
import models.behaviors.{BehaviorResponse, BotResult, SimpleTextResult, TextWithAttachmentsResult}
import services.DefaultServices
import utils.Color

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

@Singleton
class EventHandler @Inject() (
                               services: DefaultServices,
                               implicit val ec: ExecutionContext
                             ) {

  val dataService = services.dataService
  val lambdaService = services.lambdaService
  val cacheService = services.cacheService
  implicit val actorService = services.actorSystem

  def slackEventService = services.slackEventService

  def startInvokeConversationFor(event: Event): Future[Seq[BotResult]] = {
    for {
      maybeTeam <- dataService.teams.find(event.teamId)
      responses <- dataService.behaviorResponses.allFor(event, maybeTeam, None)
      results <- Future.sequence(responses.map(_.result)).flatMap { r =>
        if (r.isEmpty && event.isResponseExpected) {
          event.noExactMatchResult(services).map { noMatchResult =>
            Seq(noMatchResult)
          }
        } else {
          Future.successful(r)
        }
      }
    } yield results
  }

  def cancelConversationResult(event: Event, conversation: Conversation, withMessage: String): Future[BotResult] = {
    conversation.cancel(dataService).map { _ =>
      SimpleTextResult(event, Some(conversation), withMessage, forcePrivateResponse = false)
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

  def handleInConversation(originalConvo: Conversation, event: Event): Future[BotResult] = {
    dataService.conversations.touch(originalConvo).flatMap { updatedConvo =>
      if (isCancelConversationMessage(event)) {
        cancelConversationResult(event, updatedConvo, s"OK, I’ll stop asking about that.")
      } else {
        if (originalConvo.isStale) {
          updatedConvo.maybeNextParamToCollect(event, services).map { maybeNextParam =>
            val maybeLastPrompt = maybeNextParam.map { nextParam =>
              nextParam.input.question
            }
            val key = updatedConvo.pendingEventKey
            services.cacheService.cacheEvent(key, event, 5.minutes)
            val actionList = Seq(
              SlackMessageActionButton(CONFIRM_CONTINUE_CONVERSATION, "Yes, this is my answer", updatedConvo.id),
              SlackMessageActionButton(DONT_CONTINUE_CONVERSATION, "No, it’s not an answer", updatedConvo.id)
            )
            val prompt = maybeLastPrompt.map { lastPrompt =>
              s"""It’s been a while since I asked you this question:
                 |
                 |> $lastPrompt
                 |""".stripMargin
            }.getOrElse {
              s"It’s been a while since I asked you the question above."
            } + s"\n\nJust so I’m sure, is this an answer?"
            val actions = SlackMessageActionsGroup("should_continue_conversation", actionList, Some(event.relevantMessageTextWithFormatting), Some(Color.PINK))
            TextWithAttachmentsResult(event, Some(updatedConvo), prompt, forcePrivateResponse = false, Seq(actions))
          }
        } else {
          updatedConvo.resultFor(event, services)
        }
      }
    }
  }

  def handle(event: Event, maybeConversation: Option[Conversation]): Future[Seq[BotResult]] = {
    maybeConversation.map { conversation =>
      handleInConversation(conversation, conversation.maybeOriginalEventType.map { eventType =>
        event.withOriginalEventType(eventType)
      }.getOrElse(event)).map(Seq(_))
    }.getOrElse {
      BuiltinBehavior.maybeFrom(event, services).map { builtin =>
        builtin.result.map(Seq(_))
      }.getOrElse {
        startInvokeConversationFor(event)
      }
    }
  }
}
