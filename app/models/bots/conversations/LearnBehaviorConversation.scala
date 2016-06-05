package models.bots.conversations

import models.IDs
import models.bots._
import models.bots.triggers.MessageTriggerQueries
import org.joda.time.DateTime
import services.AWSLambdaService
import slick.driver.PostgresDriver.api._
import utils.SlackCodeFixer
import scala.concurrent.ExecutionContext.Implicits.global

case class LearnBehaviorConversation(
                         id: String,
                         behavior: Behavior,
                         context: String, // Slack, etc
                         userIdForContext: String, // id for Slack, etc user
                         startedAt: DateTime,
                         state: String = Conversation.NEW_STATE
                         ) extends Conversation {

  val conversationType = Conversation.LEARN_BEHAVIOR

  val NEW_PROMPT = "OK, teach me. You can go back and change any of this later."

  val DESCRIPTION_PROMPT = "Describe the new behavior you'd like me to learn:"

  import services.AWSLambdaConstants._

  def codePromptFor(lambdaService: AWSLambdaService) = {
    val editPromptString = "describe how this should work"
    val editPrompt = behavior.editLinkFor(lambdaService.configuration).map { link =>
      s"<$link|$editPromptString>"
    }.getOrElse(editPromptString)
    s"""Great. Next $editPrompt.
      |
      |Or, if you like, you can write a quick node.js function right here.
      |
      |Parameters other than $ON_SUCCESS_PARAM, $ON_ERROR_PARAM and $CONTEXT_PARAM will be supplied by the user. For example:
      |
      |```function(someNumber, someOtherNumber, $ON_SUCCESS_PARAM, $ON_ERROR_PARAM, $CONTEXT_PARAM) { $ON_SUCCESS_PARAM(someNumber + someOtherNumber); }```
      |
      |""".stripMargin
    }

  val TRIGGER_PROMPT = """What kinds of things do people need to say to trigger this behavior?
                         |
                         |List some trigger phrases, comma-separated:
                         |
                         |""".stripMargin

  private def looksLikeCode(message: String): Boolean = {
    """^\s*function\s*\(.*""".r.findFirstMatchIn(message).nonEmpty
  }

  private def paramPromptFor(param: BehaviorParameter): String = {
    s"To collect a value for `${param.name}`, what question should @ellipsis ask the user?"
  }

  private def updateStateTo(newState: String): DBIO[Conversation] = {
    this.copy(state = newState).save
  }

  private def collectDescriptionFrom(message: String): DBIO[Conversation] = {
    for {
      _ <- behavior.copy(maybeDescription = Some(message)).save
      updated <- updateStateTo(LearnBehaviorConversation.COLLECT_CODE_STATE)
    } yield updated
  }

  private def collectCodeFrom(message: String, lambdaService: AWSLambdaService): DBIO[Conversation] = {
    import LearnBehaviorConversation._

    for {
      params <- behavior.learnCode(SlackCodeFixer.runFor(message), lambdaService)
      newState <- DBIO.successful(if (params.isEmpty) { COLLECT_TRIGGERS_STATE } else { COLLECT_PARAMS_STATE })
      updated <- updateStateTo(newState)
    } yield updated
  }

  private def collectParamsFrom(message: String): DBIO[Conversation] = {
    for {
      maybeNextParam <- BehaviorParameterQueries.nextIncompleteFor(behavior)
      _ <- maybeNextParam.map { param =>
        param.copy(maybeQuestion = Some(message)).save
      }.getOrElse(DBIO.successful(Unit))
      updatedConversation <- BehaviorParameterQueries.nextIncompleteFor(behavior).flatMap { maybeParam =>
        maybeParam.map { param =>
          DBIO.successful(this)
        }.getOrElse {
          updateStateTo(LearnBehaviorConversation.COLLECT_TRIGGERS_STATE)
        }
      }
    } yield updatedConversation
  }

  private def collectTriggersFrom(message: String): DBIO[Conversation] = {
    val phrases = message.split("""\s*,\s*""").toSeq
    for {
      _ <- DBIO.sequence(phrases.map { phrase =>
        MessageTriggerQueries.ensureFor(behavior, phrase)
      })
      updatedConversation <- updateStateTo(Conversation.DONE_STATE)
    } yield updatedConversation
  }

  def updateWith(event: Event, lambdaService: AWSLambdaService): DBIO[Conversation] = {
    import Conversation._
    import LearnBehaviorConversation._

    event match {
      case e: SlackMessageEvent => {
        val message = e.context.relevantMessageText
        state match {
          case NEW_STATE => updateStateTo(COLLECT_DESCRIPTION_STATE)
          case COLLECT_DESCRIPTION_STATE => collectDescriptionFrom(message)
          case COLLECT_CODE_STATE => collectCodeFrom(message, lambdaService)
          case COLLECT_PARAMS_STATE => collectParamsFrom(message)
          case COLLECT_TRIGGERS_STATE => collectTriggersFrom(message)
          case DONE_STATE => DBIO.successful(this)
        }
      }
      case _ => DBIO.successful(this)
    }
  }

  def respond(event: Event, lambdaService: AWSLambdaService): DBIO[Unit] = {
    import Conversation._
    import LearnBehaviorConversation._

    val eventualReply = state match {
      case NEW_STATE => DBIO.successful(NEW_PROMPT)
      case COLLECT_DESCRIPTION_STATE => DBIO.successful(DESCRIPTION_PROMPT)
      case COLLECT_CODE_STATE => DBIO.successful(codePromptFor(lambdaService))
      case COLLECT_PARAMS_STATE => {
        BehaviorParameterQueries.nextIncompleteFor(behavior).map { maybeParam =>
          maybeParam.map { param =>
            paramPromptFor(param)
          }.getOrElse("All done!")
        }
      }
      case COLLECT_TRIGGERS_STATE => DBIO.successful(TRIGGER_PROMPT)
      case DONE_STATE => DBIO.successful("Done!")
    }

    eventualReply.map { reply =>
      event.context.sendMessage(reply)
    }
  }

}

object LearnBehaviorConversation {

  val COLLECT_DESCRIPTION_STATE = "collect_description"
  val COLLECT_CODE_STATE = "collect_code"
  val COLLECT_PARAMS_STATE = "collect_params"
  val COLLECT_TRIGGERS_STATE = "collect_triggers"

  def createFor(
                 behavior: Behavior,
                 context: String,
                 userIdForContext: String
                 ): DBIO[LearnBehaviorConversation] = {
    val newInstance = LearnBehaviorConversation(IDs.next, behavior, context, userIdForContext, DateTime.now, Conversation.NEW_STATE)
    newInstance.save.map(_ => newInstance)
  }

  def endAllFor(behavior: Behavior): DBIO[Unit] = {
    ConversationQueries.all.
      filter(_.behaviorId === behavior.id).
      filter(_.conversationType === Conversation.LEARN_BEHAVIOR).
      map(_.state).
      update(Conversation.DONE_STATE).
      map(_ => Unit)
  }
}
