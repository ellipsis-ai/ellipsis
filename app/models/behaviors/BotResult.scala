package models.behaviors

import akka.actor.ActorSystem
import json.Formatting._
import models.IDs
import models.accounts.OAuth2State
import models.accounts.logintoken.LoginToken
import models.accounts.oauth1application.OAuth1Application
import models.accounts.oauth2application.OAuth2Application
import models.accounts.user.User
import models.behaviors.ResultType.ResultType
import models.behaviors.behaviorversion.{BehaviorResponseType, BehaviorVersion, Normal, Private}
import models.behaviors.config.requiredoauth1apiconfig.RequiredOAuth1ApiConfig
import models.behaviors.config.requiredoauth2apiconfig.RequiredOAuth2ApiConfig
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events._
import models.behaviors.templates.TemplateApplier
import models.team.Team
import play.api.Configuration
import play.api.libs.json._
import play.api.mvc.Call
import services.AWSLambdaConstants._
import services.caching.CacheService
import services.{AWSLambdaLogResult, DataService, DefaultServices}
import slick.dbio.DBIO
import utils.UploadFileSpec

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

object ResultType extends Enumeration {
  type ResultType = Value
  val Success, SimpleText, ActionAcknowledgment, TextWithActions, ConversationPrompt, NoResponse, ExecutionError, SyntaxError, NoCallbackTriggered, MissingTeamEnvVar, AWSDown, OAuth2TokenMissing, RequiredApiNotReady, AdminSkillErrorNotification = Value
}

trait WithActionArgs {
  val args: Option[Seq[ActionArg]]
  val argumentsMap: Map[String, String] = {
    args.getOrElse(Seq()).map { ea =>
      (ea.name, ea.value)
    }.toMap
  }
}

case class ActionArg(name: String, value: String)

case class NextAction(actionName: String, args: Option[Seq[ActionArg]]) extends WithActionArgs

case class SkillCodeActionChoice(
                                   label: String,
                                   actionName: String,
                                   args: Option[Seq[ActionArg]],
                                   allowOthers: Option[Boolean],
                                   allowMultipleSelections: Option[Boolean],
                                   quiet: Option[Boolean]
                               ) extends WithActionArgs {
  def toActionChoiceWith(user: User, behaviorVersion: BehaviorVersion): ActionChoice = {
    ActionChoice(
      label,
      actionName,
      args,
      allowOthers,
      allowMultipleSelections,
      user.id,
      behaviorVersion.id,
      quiet
    )
  }
}

case class ActionChoice(
                         label: String,
                         actionName: String,
                         args: Option[Seq[ActionArg]],
                         allowOthers: Option[Boolean],
                         allowMultipleSelections: Option[Boolean],
                         userId: String,
                         originatingBehaviorVersionId: String,
                         quiet: Option[Boolean]
                       ) extends WithActionArgs {

  val areOthersAllowed: Boolean = allowOthers.contains(true)

  val shouldBeQuiet: Boolean = quiet.contains(true)

  private def isAllowedBecauseAdmin(user: User, dataService: DataService)(implicit ec: ExecutionContext): Future[Boolean] = {
    dataService.users.isAdmin(user).map { isAdmin =>
      areOthersAllowed && isAdmin
    }
  }

  def canBeTriggeredBy(user: User, userTeamIdForContext: String, botTeamIdForContext: String, dataService: DataService)(implicit ec: ExecutionContext): Future[Boolean] = {
    val sameUser = userId == user.id
    val sameTeam = areOthersAllowed && userTeamIdForContext == botTeamIdForContext
    isAllowedBecauseAdmin(user, dataService).map { admin =>
      sameUser || sameTeam || admin
    }
  }

}

sealed trait BotResult {
  val resultType: ResultType.Value
  val responseType: BehaviorResponseType
  val event: Event
  val maybeConversation: Option[Conversation]
  val maybeBehaviorVersion: Option[BehaviorVersion]
  def maybeNextAction: Option[NextAction] = None
  def maybeChoicesAction(dataService: DataService)(implicit ec: ExecutionContext): DBIO[Option[Seq[ActionChoice]]] = DBIO.successful(None)
  val shouldInterrupt: Boolean = true
  def text: String
  def fullText: String = text
  def hasText: Boolean = fullText.trim.nonEmpty
  val developerContext: DeveloperContext
  def maybeLog: Option[String] = None
  def maybeLogFile: Option[UploadFileSpec] = None

  def shouldIncludeLogs: Boolean = {
    maybeLog.isDefined && (developerContext.isInDevMode || developerContext.isInInvocationTester)
  }

  def files: Seq[UploadFileSpec] = {
    if (shouldIncludeLogs) {
      Seq(maybeLogFile).flatten
    } else {
      Seq()
    }
  }

  def filesAsLogText: String = {
    if (files.nonEmpty) {
      files.map { fileSpec =>
        val filename = fileSpec.filename.getOrElse("File")
        val filetype = fileSpec.filetype.getOrElse("unknown type")
        val content = fileSpec.content.map { content =>
          val lines = content.split("\n")
          if (lines.length > 10) {
            lines.slice(0, 10).mkString("", "\n", "\n...(truncated)")
          } else {
            lines.mkString("\n")
          }
        }.getOrElse("(empty)")
        s"""$filename ($filetype):
           |$content
           """.stripMargin
      }.mkString("======\nFiles:\n======\n", "\n======\n", "\n======\n")
    } else {
      ""
    }
  }

  val interruptionPrompt = {
    val action = if (maybeConversation.isDefined) { "ask" } else { "tell" }
    s"You haven't answered my question yet, but I have something new to $action you."
  }

  def interruptOngoingConversationsForAction(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Boolean] = {
    if (maybeConversation.exists(_.maybeThreadId.isDefined)) {
      DBIO.successful(false)
    } else {
      val dataService = services.dataService
      for {
        ongoing <- dataService.conversations.allOngoingForAction(event.eventContext, Some(this))
        ongoingWithRoots <- DBIO.sequence(ongoing.map { ea =>
          dataService.parentConversations.rootForAction(ea).map { root =>
            (ea, root)
          }
        })
        maybeRoot <- maybeConversation.map { convo =>
          dataService.parentConversations.rootForAction(convo).map(Some(_))
        }.getOrElse(DBIO.successful(None))
        interruptionResults <- {
          val toInterrupt =
            ongoingWithRoots.
              filterNot { case(_, root) => maybeRoot.map(_.id).contains(root.id) }.
              map { case(convo, _) => convo }
          DBIO.sequence(toInterrupt.map { ea =>
            dataService.conversations.backgroundAction(ea, interruptionPrompt, includeUsername = true)
          })
        }
      } yield interruptionResults.nonEmpty
    }
  }

  def beforeSend(): Unit = {}

  val shouldSend: Boolean = true

  def attachments: Seq[MessageAttachment] = Seq()

  def isForManagedGroup(dataService: DataService)(implicit ec: ExecutionContext): Future[Boolean] = {
    maybeBehaviorVersion.map { behaviorVersion =>
      dataService.managedBehaviorGroups.maybeFor(behaviorVersion.group).map(_.isDefined)
    }.getOrElse(Future.successful(false))
  }

  def shouldNotifyAdmins(implicit ec: ExecutionContext): Future[Boolean] = Future.successful(false)

}

trait BotResultWithLogResult extends BotResult {
  val payloadJson: JsValue
  val maybeLogResult: Option[AWSLambdaLogResult]

  private val maybeAuthorLogTextFromJson: Option[String] = {
    val logged = (payloadJson \ "logs").validate[Seq[ExecutionLogData]] match {
      case JsSuccess(logs, _) => logs.map(_.toString).mkString("\n")
      case JsError(_) => ""
    }
    Option(logged).filter(_.nonEmpty)
  }

  val maybeAuthorLog: Option[String] = {
    maybeAuthorLogTextFromJson orElse {
      maybeLogResult.map(_.authorDefinedLogStatements).filter(_.nonEmpty)
    }
  }

  val maybeAuthorLogFile: Option[UploadFileSpec] = {
    maybeAuthorLog.map { log =>
      UploadFileSpec(Some(log), Some("text"), Some("Developer log"))
    }
  }

  override def maybeLog: Option[String] = maybeAuthorLog
  override def maybeLogFile: Option[UploadFileSpec] = maybeAuthorLogFile

}

case class InvalidFilesException(message: String) extends Exception {
  def responseText: String =
    s"""Invalid files passed to `ellipsis.success()`
       |
       |Errors: $message
       |
       |The value for the `files` property should be an array like:
       |
       |```
       |[
       |  {
       |    content: "The content…",
       |    filetype: "text",
       |    filename: "filname.txt"
       |  }, ...
       |]
       |```
     """.stripMargin
}

case class SuccessResult(
                          event: Event,
                          behaviorVersion: BehaviorVersion,
                          maybeConversation: Option[Conversation],
                          result: JsValue,
                          payloadJson: JsValue,
                          parametersWithValues: Seq[ParameterWithValue],
                          invocationJson: JsObject,
                          maybeResponseTemplate: Option[String],
                          maybeLogResult: Option[AWSLambdaLogResult],
                          override val responseType: BehaviorResponseType,
                          developerContext: DeveloperContext
                        ) extends BotResultWithLogResult {

  val resultType = ResultType.Success

  val maybeBehaviorVersion: Option[BehaviorVersion] = Some(behaviorVersion)

  override def files: Seq[UploadFileSpec] = {
    val authoredFiles = (payloadJson \ "files").validateOpt[Seq[UploadFileSpec]] match {
      case JsSuccess(maybeFiles, _) => maybeFiles.getOrElse(Seq())
      case JsError(errs) => throw InvalidFilesException(errs.map { case (_, validationErrors) =>
        validationErrors.map(_.message).mkString(", ")
      }.mkString(", "))
    }
    authoredFiles ++ super.files
  }

  override def maybeNextAction: Option[NextAction] = {
    (payloadJson \ "next").asOpt[NextAction]
  }

  override def maybeChoicesAction(dataService: DataService)(implicit ec: ExecutionContext): DBIO[Option[Seq[ActionChoice]]] = {
    event.ensureUserAction(dataService).map { user =>
      val maybeChoices = (payloadJson \ "choices").asOpt[Seq[SkillCodeActionChoice]]
      maybeChoices.map { choices =>
        choices.map(_.toActionChoiceWith(user, behaviorVersion))
      }
    }
  }

  def text: String = {
    val inputs = invocationJson.fields ++ parametersWithValues.map { ea => (ea.parameter.name, ea.preparedValue) }
    TemplateApplier(maybeResponseTemplate, JsDefined(result), inputs).apply
  }
}

case class SimpleTextResult(
                             event: Event,
                             maybeConversation: Option[Conversation],
                             simpleText: String,
                             override val responseType: BehaviorResponseType,
                             override val shouldInterrupt: Boolean = true
                           ) extends BotResult {

  val developerContext: DeveloperContext = DeveloperContext.default

  val resultType = ResultType.SimpleText

  val maybeBehaviorVersion: Option[BehaviorVersion] = None

  def text: String = simpleText

}

case class TextWithAttachmentsResult(
                                      event: Event,
                                      maybeConversation: Option[Conversation],
                                      simpleText: String,
                                      override val responseType: BehaviorResponseType,
                                      override val attachments: Seq[MessageAttachment]
                                    ) extends BotResult {
  val resultType = ResultType.TextWithActions

  val maybeBehaviorVersion: Option[BehaviorVersion] = None

  val developerContext: DeveloperContext = DeveloperContext.default

  def text: String = simpleText
}

case class NoResponseResult(
                             event: Event,
                             behaviorVersion: BehaviorVersion,
                             maybeConversation: Option[Conversation],
                             payloadJson: JsValue,
                             maybeLogResult: Option[AWSLambdaLogResult]
                           ) extends BotResultWithLogResult {

  val developerContext: DeveloperContext = DeveloperContext.default

  val resultType = ResultType.NoResponse
  val responseType: BehaviorResponseType = Normal // N/A
  override val shouldInterrupt = false

  val maybeBehaviorVersion: Option[BehaviorVersion] = Some(behaviorVersion)

  def text: String = ""

  override val shouldSend: Boolean = false
}

trait WithBehaviorLink {

  val behaviorVersion: BehaviorVersion
  val maybeBehaviorVersion: Option[BehaviorVersion] = Some(behaviorVersion)
  val dataService: DataService
  val configuration: Configuration
  val responseType: BehaviorResponseType = behaviorVersion.responseType
  val team: Team = behaviorVersion.team

  def link: String = dataService.behaviors.editLinkFor(behaviorVersion.group.id, Some(behaviorVersion.behavior.id), configuration)

  def linkToBehaviorFor(text: String): String = {
    s"[$text](${link})"
  }

  def teamLink: String = {
    val baseUrl = configuration.get[String]("application.apiBaseUrl")
    val path = controllers.routes.ApplicationController.index(Some(team.id))
    val url = s"$baseUrl$path"
    s"[${team.name}](${url})"
  }

}

case class ExecutionErrorResult(
                                 event: Event,
                                 maybeConversation: Option[Conversation],
                                 behaviorVersion: BehaviorVersion,
                                 dataService: DataService,
                                 configuration: Configuration,
                                 payloadJson: JsValue,
                                 maybeLogResult: Option[AWSLambdaLogResult],
                                 developerContext: DeveloperContext
                               ) extends BotResultWithLogResult with WithBehaviorLink {

  val resultType = ResultType.ExecutionError
  val howToIncludeStackTraceMessage = "\n\nTo include a stack trace, throw an `Error` object in your code. For example:\n  throw new Error(\"Something went wrong.\")"

  private val maybeError: Option[ExecutionErrorData] = {
    (payloadJson \ "error").validate[ExecutionErrorData] match {
      case JsSuccess(errorValue, _) => Some(errorValue)
      case JsError(_) => None
    }
  }

  private val maybeUserErrorMessage: Option[String] = {
    maybeError.flatMap(_.userMessage)
  }

  def text: String = {
    s"I encountered an error in ${linkToBehaviorFor("one of your skills")}${
      maybeUserErrorMessage.map(userError => s":\n\n$userError").getOrElse(".")
    }"
  }

  private def logContainsStackTrace(log: String): Boolean = {
    val lines = log.lines.toList
    lines.length > 1 && lines.tail.exists(_.matches("""^\s*at .+?\(.+?:\d+:\d+\)"""))
  }

  private def dropEnclosingDoubleQuotes(text: String): String = """^"|"$""".r.replaceAllIn(text, "")

  private def processedResultFor(result: JsValue): String = {
    dropEnclosingDoubleQuotes(result.as[String])
  }

  private val maybeCallbackErrorMessage: Option[String] = {
    (payloadJson \ "errorMessage").toOption.map(processedResultFor).filterNot {
      _.matches("""RequestId: \S+ Process exited before completing request""")
    }
  }

  private val maybeThrownLogMessage: Option[String] = {
    maybeLogResult.flatMap(_.maybeTranslated)
  }

  private val maybeErrorLog: Option[String] = {
    maybeError.map { error =>
      val translatedStack = error.translateStack
      if (translatedStack.nonEmpty) {
        translatedStack
      } else {
        error.message + howToIncludeStackTraceMessage
      }
    } orElse {
      val result = Seq(maybeCallbackErrorMessage, maybeThrownLogMessage).flatten.mkString("\n")
      Option(result).filter(_.nonEmpty)
    }
  }

  override def maybeLog: Option[String] = {
    if (maybeAuthorLog.isEmpty && maybeErrorLog.isEmpty) {
      None
    } else {
      Some(maybeAuthorLog.map(_ + "\n").getOrElse("") + maybeErrorLog.getOrElse(""))
    }
  }
  override def maybeLogFile: Option[UploadFileSpec] = {
    maybeLog.map { log =>
      UploadFileSpec(Some(log), Some("text"), Some("Developer log"))
    }
  }

  override def shouldNotifyAdmins(implicit ec: ExecutionContext): Future[Boolean] = isForManagedGroup(dataService)

}

case class SyntaxErrorResult(
                              event: Event,
                              maybeConversation: Option[Conversation],
                              behaviorVersion: BehaviorVersion,
                              dataService: DataService,
                              configuration: Configuration,
                              payloadJson: JsValue,
                              maybeLogResult: Option[AWSLambdaLogResult],
                              developerContext: DeveloperContext
                            ) extends BotResultWithLogResult with WithBehaviorLink {

  val resultType = ResultType.SyntaxError

  def text: String = {
    s"""
       |There's a syntax error in your skill:
       |
       |````
       |${(payloadJson \ "errorMessage").asOpt[String].getOrElse("")}
       |````
       |
       |${linkToBehaviorFor("Take a look in the skill editor")} for more details.
     """.stripMargin
  }

  override def shouldNotifyAdmins(implicit ec: ExecutionContext): Future[Boolean] = isForManagedGroup(dataService)

}

case class NoCallbackTriggeredResult(
                                      event: Event,
                                      maybeConversation: Option[Conversation],
                                      behaviorVersion: BehaviorVersion,
                                      dataService: DataService,
                                      configuration: Configuration,
                                      developerContext: DeveloperContext
                                    ) extends BotResult with WithBehaviorLink {

  val resultType = ResultType.NoCallbackTriggered

  def text = s"It looks like neither callback was triggered in ${linkToBehaviorFor("your skill")}— you need to make sure that `$SUCCESS_CALLBACK`" ++
    s"is called to end every successful invocation and `$ERROR_CALLBACK` is called to end every unsuccessful one"

  override def shouldNotifyAdmins(implicit ec: ExecutionContext): Future[Boolean] = isForManagedGroup(dataService)

}

case class AdminSkillErrorNotificationResult(
                                              event: Event,
                                              originalResult: BotResult
                                            ) extends BotResult {

  val resultType = ResultType.AdminSkillErrorNotification
  val responseType: BehaviorResponseType = Normal

  override def shouldIncludeLogs: Boolean = true

  lazy val developerContext: DeveloperContext = originalResult.developerContext
  lazy val skillLink: String = originalResult match {
    case r: WithBehaviorLink => r.linkToBehaviorFor("✎ Edit")
    case _ => ""
  }
  lazy val teamLink: String = originalResult match {
    case r: WithBehaviorLink => r.teamLink
    case _ => ""
  }
  lazy val description: String = originalResult.maybeBehaviorVersion.map { bv =>
    val action = bv.maybeName.getOrElse(bv.id)
    val skill = bv.groupVersion.name
    s" running action `$action` in skill `$skill` $skillLink"
  }.getOrElse("")
  lazy val text: String = {
    val userIdForContext = originalResult.event.eventContext.userIdForContext
    val contextUserText = s"Context User ID: <@${userIdForContext}> (ID #${userIdForContext})"
    s"""Error$description
       |
       |Team: $teamLink
       |$contextUserText
       |Result type: ${originalResult.resultType}
       |
     """.stripMargin
  }

  lazy val maybeConversation: Option[Conversation] = None
  lazy val maybeBehaviorVersion: Option[BehaviorVersion] = originalResult.maybeBehaviorVersion
  override def maybeLogFile: Option[UploadFileSpec] = originalResult.maybeLogFile

}

case class MissingTeamEnvVarsResult(
                                 event: Event,
                                 maybeConversation: Option[Conversation],
                                 behaviorVersion: BehaviorVersion,
                                 dataService: DataService,
                                 configuration: Configuration,
                                 missingEnvVars: Set[String],
                                 botPrefix: String,
                                 developerContext: DeveloperContext
                               ) extends BotResult with WithBehaviorLink {


  val linkToEnvVarConfig: String = {
    val baseUrl = configuration.get[String]("application.apiBaseUrl")
    val path = controllers.web.settings.routes.EnvironmentVariablesController.list(Some(event.ellipsisTeamId), Some(missingEnvVars.mkString(",")))
    val url = s"$baseUrl$path"
    s"[Configure environment variables](${url})"
  }

  val resultType = ResultType.MissingTeamEnvVar

  def text = {
    s"""
       |To use ${linkToBehaviorFor("this skill")}, you need the following environment variables defined:
       |${missingEnvVars.map( ea => s"\n- $ea").mkString("")}
       |
       |$linkToEnvVarConfig
    """.stripMargin
  }
}

case class AWSDownResult(
                          event: Event,
                          behaviorVersion: BehaviorVersion,
                          maybeConversation: Option[Conversation],
                          dataService: DataService
                        ) extends BotResult {

  val resultType = ResultType.AWSDown
  val responseType: BehaviorResponseType = Normal

  val developerContext: DeveloperContext = DeveloperContext.default

  val maybeBehaviorVersion: Option[BehaviorVersion] = Some(behaviorVersion)

  def text: String = {
    """
      |The Amazon Web Service that Ellipsis relies upon is currently down.
      |
      |Try asking Ellipsis anything later to check on the status.
      |""".stripMargin
  }

  override def shouldNotifyAdmins(implicit ec: ExecutionContext): Future[Boolean] = isForManagedGroup(dataService)

}

trait OAuthTokenMissing {
  val key = IDs.next

  val resultType = ResultType.OAuth2TokenMissing

  val behaviorVersion: BehaviorVersion
  val loginToken: LoginToken
  val event: Event
  val configuration: Configuration
  val cacheService: CacheService
  val apiApplicationId: String
  val apiApplicationName: String

  val maybeBehaviorVersion: Option[BehaviorVersion] = Some(behaviorVersion)

  val responseType: BehaviorResponseType = Private

  val redirectPath: Call

  def authLink: String = {
    val baseUrl = configuration.get[String]("application.apiBaseUrl")
    val redirect = s"$baseUrl$redirectPath"
    val authPath = controllers.routes.SocialAuthController.loginWithToken(loginToken.value, Some(redirect))
    s"$baseUrl$authPath"
  }

  def text: String = {
    s"""To use this skill, you need to [authenticate with ${apiApplicationName}]($authLink).
       |
       |You only need to do this one time for ${apiApplicationName}. You may be prompted to sign in to Ellipsis using your Slack account.
       |""".stripMargin
  }

  def beforeSend: Unit = cacheService.cacheEvent(key, event, 5.minutes)
}

case class OAuth1TokenMissing(
                               oAuth1Application: OAuth1Application,
                               event: Event,
                               behaviorVersion: BehaviorVersion,
                               maybeConversation: Option[Conversation],
                               loginToken: LoginToken,
                               cacheService: CacheService,
                               configuration: Configuration,
                               developerContext: DeveloperContext
                             ) extends BotResult with OAuthTokenMissing {

  val apiApplicationId: String = oAuth1Application.id
  val apiApplicationName: String = oAuth1Application.name

  val redirectPath: Call = controllers.routes.APIAccessController.linkCustomOAuth1Service(apiApplicationId, Some(key), None)

  override def beforeSend: Unit = super.beforeSend
}

case class OAuth2TokenMissing(
                               oAuth2Application: OAuth2Application,
                               event: Event,
                               behaviorVersion: BehaviorVersion,
                               maybeConversation: Option[Conversation],
                               loginToken: LoginToken,
                               cacheService: CacheService,
                               configuration: Configuration,
                               developerContext: DeveloperContext
                             ) extends BotResult with OAuthTokenMissing {

  val apiApplicationId: String = oAuth2Application.id
  val apiApplicationName: String = oAuth2Application.name

  val state: String = OAuth2State(IDs.next, Some(key), None).encodedString

  val redirectPath: Call = controllers.routes.APIAccessController.linkCustomOAuth2Service(apiApplicationId, None, Some(state))

  override def beforeSend: Unit = super.beforeSend
}

trait RequiredApiNotReady {
  val resultType: ResultType = ResultType.RequiredApiNotReady
  val responseType: BehaviorResponseType = Private

  val behaviorVersion: BehaviorVersion
  val maybeBehaviorVersion: Option[BehaviorVersion] = Some(behaviorVersion)
  val dataService: DataService
  val configuration: Configuration

  val requiredApiName: String

  def configLink: String = dataService.behaviors.editLinkFor(behaviorVersion.groupVersion.group.id, None, configuration)
  def configText: String = {
    s"You first must [configure the ${requiredApiName} API]($configLink)"
  }

  def text: String = {
    s"This skill is not ready to use. $configText."
  }
}

case class RequiredOAuth1ApiNotReady(
                                required: RequiredOAuth1ApiConfig,
                                event: Event,
                                behaviorVersion: BehaviorVersion,
                                maybeConversation: Option[Conversation],
                                dataService: DataService,
                                configuration: Configuration,
                                developerContext: DeveloperContext
                             ) extends BotResult with RequiredApiNotReady {
  val requiredApiName: String = required.api.name
}

case class RequiredOAuth2ApiNotReady(
                                      required: RequiredOAuth2ApiConfig,
                                      event: Event,
                                      behaviorVersion: BehaviorVersion,
                                      maybeConversation: Option[Conversation],
                                      dataService: DataService,
                                      configuration: Configuration,
                                      developerContext: DeveloperContext
                                    ) extends BotResult with RequiredApiNotReady {
  val requiredApiName: String = required.api.name
}
