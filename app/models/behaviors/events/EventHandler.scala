package models.behaviors.events

import javax.inject._
import json.SlackUserData
import models.behaviors.behaviorparameter.FetchValidValuesBadResultException
import models.behaviors.builtins.BuiltinBehavior
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.SlackMessageActionConstants._
import models.behaviors.{BotResult, SimpleTextResult, TextWithAttachmentsResult}
import services.DefaultServices
import utils.Color

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

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
    val isExactMenuItemText = Conversation.CANCEL_MENU_ITEM_TEXT == text
    /*
    One could imagine allowing the stop words to work with no prefix in a DM with Ellipsis, but since such words
    could also be valid answers, disabling this for now.

    val isDMPlusKeyword = mentionedBot && "^(stop|cancel|skip)$".r.findFirstIn(text).isDefined */
    shortcutPlusKeyword || mentionedPlusKeyword || isExactMenuItemText /* || isDMPlusKeyword */
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
            val callbackId = continueConversationCallbackIdFor(event.userIdForContext, Some(updatedConvo.id))
            val actionList = Seq(
              SlackMessageActionButton(callbackId, "Yes, this is my answer", YES),
              SlackMessageActionButton(callbackId, "No, it’s not an answer", NO)
            )
            val prompt = maybeLastPrompt.map { lastPrompt =>
              s"""It’s been a while since I asked you this question:
                 |
                 |> $lastPrompt
                 |""".stripMargin
            }.getOrElse {
              s"It’s been a while since I asked you the question above."
            } + s"\n\nJust so I’m sure, is this an answer?"
            val maybeSlackUserList = event match {
              case slackMessageEvent: SlackMessageEvent => Some(slackMessageEvent.message.userList)
              case _ => None
            }
            val actions = SlackMessageActionsGroup(callbackId, actionList, Some(event.relevantMessageTextWithFormatting), maybeSlackUserList, Some(Color.PINK))
            TextWithAttachmentsResult(event, Some(updatedConvo), prompt, forcePrivateResponse = false, Seq(actions))
          }
        } else {
          updatedConvo.resultFor(event, services)
        }
      }
    }.recoverWith {
      case e: FetchValidValuesBadResultException => dataService.conversations.cancel(originalConvo).map(_ => e.result)
    }
  }

  def maybeHandleInExpiredThread(event: Event): Future[Option[BotResult]] = {
    event match {
      case e: SlackMessageEvent => {
        e.maybeThreadId.map { threadId =>
          dataService.conversations.maybeWithThreadId(threadId, e.userIdForContext, e.context).map { maybeConvo =>
            maybeConvo.flatMap { convo =>
              if (convo.isDone) {
                val channelText = if (e.isDirectMessage) {
                  "the DM channel"
                } else {
                  event.maybeChannel.map { channel =>
                    s"<#$channel>"
                  }.getOrElse("the main channel")
                }
                Some(SimpleTextResult(event, Some(convo), s"This conversation is either done or has expired. You can start a new one back in $channelText.", forcePrivateResponse = false))
              } else {
                None
              }
            }
          }
        }.getOrElse(Future.successful(None))
      }
      case _ => Future.successful(None)
    }

  }

  def handle(event: Event, maybeConversation: Option[Conversation]): Future[Seq[BotResult]] = {
    (maybeConversation.map { conversation =>
      val isUninterrupted = cacheService.isLastConversationIdFor(event, conversation.id)
      cacheService.updateLastConversationIdFor(event, conversation.id)
      handleInConversation(conversation, conversation.maybeOriginalEventType.map { eventType =>
        event.withOriginalEventType(eventType, isUninterrupted)
      }.getOrElse(event)).map(Seq(_))
    }.getOrElse {
      event.maybeChannel.foreach { channel =>
        cacheService.clearLastConversationId(event.teamId, channel)
      }
      BuiltinBehavior.maybeFrom(event, services).map { builtin =>
        builtin.result.map(Seq(_))
      }.getOrElse {
        maybeHandleInExpiredThread(event).flatMap { maybeExpiredThreadResult =>
          maybeExpiredThreadResult.map(r => Future.successful(Seq(r))).getOrElse {
            startInvokeConversationFor(event)
          }
        }
      }
    }).recover {
      case e: FetchValidValuesBadResultException => Seq(e.result)
    }
  }
}
