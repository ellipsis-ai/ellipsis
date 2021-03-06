package models.behaviors.events

import javax.inject._
import models.behaviors._
import models.behaviors.behaviorparameter.FetchValidValuesBadResultException
import models.behaviors.behaviorversion.Normal
import models.behaviors.builtins.{BuiltinBehavior, DisplayHelpBehavior}
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.MessageActionConstants._
import models.behaviors.events.slack.SlackMessageEvent
import play.api.Logger
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
      maybeTeam <- dataService.teams.find(event.ellipsisTeamId)
      responses <- dataService.behaviorResponses.allFor(event, maybeTeam, None)
      maybeBuiltin <- BuiltinBehavior.maybeFrom(event, services)
      behaviorResults <- {
        val responsesIncludeHelpAction = responses.exists(_.behaviorVersion.isHelpAction)
        val builtinBehaviors = Seq(maybeBuiltin).flatten.filter {
          case _: DisplayHelpBehavior => !responsesIncludeHelpAction
          case _ => true
        }
        val eventualResults = Future.sequence(responses.map(_.result) ++ builtinBehaviors.map(_.result))
        if (responses.exists(_.userExpectsResponse) || builtinBehaviors.nonEmpty) {
          event.resultReactionHandler(eventualResults, services)
        }
        eventualResults
      }
      results <- {
        if (shouldReportNoExactMatch(behaviorResults, event)) {
          event.noExactMatchResult(services).map { noMatchResult =>
            Seq(noMatchResult)
          }
        } else {
          Future.successful(behaviorResults)
        }
      }
    } yield results
  }

  def shouldReportNoExactMatch(results: Seq[BotResult], event: Event): Boolean = {
    val sendingResults = results.exists(_.shouldSend)
    val nonCopilotResults = !results.forall(_.isForCopilot)
    !sendingResults && event.isResponseExpected || !nonCopilotResults && event.maybeScheduled.isDefined
  }

  def cancelConversationResult(event: Event, conversation: Conversation, withMessage: String): Future[BotResult] = {
    dataService.run(dataService.parentConversations.ancestorsForAction(conversation)).flatMap { ancestors =>
      Future.sequence((conversation :: ancestors).map { ea =>
        dataService.run(ea.cancelAction(dataService))
      }).map { _ =>
        SimpleTextResult(event, Some(conversation), withMessage, responseType = Normal)
      }
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
          updatedConvo.maybeNextParamToCollect(event, services).flatMap { maybeNextParam =>
            val maybeLastPrompt = maybeNextParam.map { nextParam =>
              nextParam.input.question
            }
            val key = updatedConvo.pendingEventKey
            services.cacheService.cacheEvent(key, event, 5.minutes)
            val callbackId = continueConversationCallbackIdFor(event.eventContext.userIdForContext, Some(updatedConvo.id))
            val eventContext = event.eventContext
            val actionList = Seq(
              eventContext.messageActionButtonFor(callbackId, "Yes, this is my answer", YES),
              eventContext.messageActionButtonFor(callbackId, "No, it’s not an answer", NO)
            )
            val prompt = maybeLastPrompt.map { lastPrompt =>
              s"""It’s been a while since I asked you this question:
                 |
                 |> $lastPrompt
                 |""".stripMargin
            }.getOrElse {
              s"It’s been a while since I asked you the question above."
            } + s"\n\nJust so I’m sure, is this an answer?"

            event.messageUserDataList(Some(updatedConvo), services).map { userDataList =>
              val attachment = eventContext.messageAttachmentFor(
                maybeText = Some(event.relevantMessageTextWithFormatting),
                maybeUserDataList = Some(userDataList),
                maybeColor = Some(Color.PINK),
                maybeCallbackId = Some(callbackId),
                actions = actionList
              )
              TextWithAttachmentsResult(event, Some(updatedConvo), prompt, responseType = Normal, Seq(attachment))
            }
          }
        } else {
          val eventualResult = dataService.run(updatedConvo.resultForAction(event, services))
          event.resultReactionHandler(eventualResult.map(Seq(_)), services)
          eventualResult
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
          dataService.conversations.maybeWithThreadId(threadId, e.eventContext.userIdForContext, e.eventContext.name).map { maybeConvo =>
            maybeConvo.flatMap { convo =>
              if (convo.isDone) {
                val channelText = if (e.eventContext.isDirectMessage) {
                  "the DM channel"
                } else {
                  event.maybeChannel.map { channel =>
                    s"<#$channel>"
                  }.getOrElse("the main channel")
                }
                Some(SimpleTextResult(event, Some(convo), s"This conversation is either done or has expired. You can start a new one back in $channelText.", responseType = Normal))
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
      for {
        userDataList <- event.messageUserDataList(services)
        _ <- Future.successful(cacheService.cacheMessageUserDataList(userDataList.toSeq, conversation.id))
        result <- dataService.run(dataService.parentConversations.rootForAction(conversation)).flatMap { root =>
          cacheService.eventHasLastConversationId(event, root.id).flatMap { hasLastConvoId =>
            val isUninterrupted = event.maybeThreadId.isDefined || hasLastConvoId
            if (event.maybeThreadId.isEmpty) {
              cacheService.updateLastConversationIdFor(event, root.id)
            }
            handleInConversation(conversation, conversation.maybeOriginalEventType.map { eventType =>
              event.withOriginalEventType(eventType, isUninterrupted)
            }.getOrElse(event)).map(Seq(_))
          }
        }
      } yield result
    }.getOrElse {
      event.maybeChannel.foreach { channel =>
        if (event.maybeThreadId.isEmpty) {
          cacheService.clearLastConversationId(event.ellipsisTeamId, channel)
        }
      }
      startInvokeConversationFor(event)
    }).recover {
      case e: FetchValidValuesBadResultException => Seq(e.result)
      case t: Throwable => {
        val msg =
          s"""Error handling event:
             |
             |${event.errorText}
           """.stripMargin
        Logger.error(msg, t)
        throw t
      }
    }
  }
}
