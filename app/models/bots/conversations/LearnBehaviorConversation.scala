package models.bots.conversations

import models.IDs
import models.bots._
import org.joda.time.DateTime
import services.AWSLambdaService
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

case class LearnBehaviorConversation(
                         id: String,
                         behavior: Behavior,
                         context: String, // Slack, etc
                         userIdForContext: String, // id for Slack, etc user
                         startedAt: DateTime,
                         state: String = LearnBehaviorConversation.NEW_STATE
                         ) extends Conversation {

  val conversationType = "learn_behavior"

  val NEW_PROMPT = "OK, teach me. You can go back and change any of this later."

  val DESCRIPTION_PROMPT = "Describe the new behavior you'd like me to learn:"

  val CODE_PROMPT = """Great. Next describe what @ellipsis should do by writing a node.js function (which can take parameters supplied by the user):
                      |
                      |e.g. function(someNumber, someOtherNumber) { return someNumber + someOtherNumber; }
                      |
                      |""".stripMargin

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
      params <- behavior.learnCode(message, lambdaService)
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
        RegexMessageTriggerQueries.ensureFor(behavior, phrase.r)
      })
      updatedConversation <- updateStateTo(LearnBehaviorConversation.DONE_STATE)
    } yield updatedConversation
  }

  def process(message: String, lambdaService: AWSLambdaService): DBIO[Conversation] = {
    import LearnBehaviorConversation._

    state match {
      case NEW_STATE => updateStateTo(LearnBehaviorConversation.COLLECT_DESCRIPTION_STATE)
      case COLLECT_DESCRIPTION_STATE => collectDescriptionFrom(message)
      case COLLECT_CODE_STATE => collectCodeFrom(message, lambdaService)
      case COLLECT_PARAMS_STATE => collectParamsFrom(message)
      case COLLECT_TRIGGERS_STATE => collectTriggersFrom(message)
      case DONE_STATE => DBIO.successful(this)
    }
  }

  def replyStringFor(message: String): DBIO[String] = {
    import LearnBehaviorConversation._

    state match {
      case NEW_STATE => DBIO.successful(NEW_PROMPT)
      case COLLECT_DESCRIPTION_STATE => DBIO.successful(DESCRIPTION_PROMPT)
      case COLLECT_CODE_STATE => DBIO.successful(CODE_PROMPT)
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
  }

  def replyFor(message: String, lambdaService: AWSLambdaService): DBIO[String] = {
    for {
      updatedConversation <- process(message, lambdaService)
      reply <- updatedConversation.replyStringFor(message)
    } yield reply
  }
}

object LearnBehaviorConversation {

  val NEW_STATE = "new"
  val COLLECT_DESCRIPTION_STATE = "collect_description"
  val COLLECT_CODE_STATE = "collect_code"
  val COLLECT_PARAMS_STATE = "collect_params"
  val COLLECT_TRIGGERS_STATE = "collect_triggers"
  val DONE_STATE = "done"

  def createFor(
                 behavior: Behavior,
                 context: String,
                 userIdForContext: String
                 ): DBIO[LearnBehaviorConversation] = {
    val newInstance = LearnBehaviorConversation(IDs.next, behavior, context, userIdForContext, DateTime.now, NEW_STATE)
    newInstance.save.map(_ => newInstance)
  }
}
