package controllers.api

import _root_.json.Formatting._
import _root_.json.{APIErrorData, APIResultWithErrorsData, APITokenData, UserData}
import akka.actor.ActorSystem
import com.google.inject.Provider
import controllers.api.context.ApiMethodContextBuilder
import controllers.api.exceptions.InvalidTokenException
import controllers.api.json.Formatting._
import controllers.api.json._
import controllers.{EllipsisController, RemoteAssets}
import javax.inject.Inject
import models.behaviors.behaviorversion.Normal
import models.behaviors.events._
import models.behaviors.{BotResultService, SimpleTextResult}
import play.api.data.Forms._
import play.api.data.{Form, FormError}
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc.{AnyContent, Request, Result}
import play.api.{Configuration, Logger}
import services.caching.CacheService
import services.slack.{SlackApiError, SlackEventService}
import services.{AWSLambdaService, DataService, DefaultServices}
import utils.{SlackFileMap, SlackMessageSenderChannelException, SlackMessageSenderException}

import scala.concurrent.{ExecutionContext, Future}

class APIController @Inject() (
                                val services: DefaultServices,
                                val eventHandler: EventHandler,
                                val assetsProvider: Provider[RemoteAssets],
                                implicit val actorSystem: ActorSystem,
                                implicit val ec: ExecutionContext
                              )
  extends EllipsisController {

  val dataService: DataService = services.dataService
  val botResultService: BotResultService = services.botResultService

  private def logAndRespondFor(status: Status, maybeErrorData: Option[APIErrorData], maybeFormErrors: Option[Seq[FormError]], details: JsValue = JsObject.empty)(implicit r: Request[AnyContent]): Result = {
    val formErrors = maybeFormErrors.map { errors =>
      errors.map { error =>
        APIErrorData(error.format, Some(error.key).filter(_.nonEmpty))
      }
    }.getOrElse(Seq())
    val errorMessage = maybeErrorData.map { data =>
      data.field.map { field =>
        s"$field: ${data.message}"
      }.getOrElse(data.message)
    }.getOrElse("")
    val errorResultData = APIResultWithErrorsData(formErrors ++ Seq(maybeErrorData).flatten)
    val jsonErrorResultData = Json.toJson(errorResultData)
    val result = status.apply(jsonErrorResultData)
    Logger.info(
      s"""Returning a ${result.header.status} for: $errorMessage
         |
         |${Json.prettyPrint(jsonErrorResultData)}
         |
         |Api info: ${Json.prettyPrint(details)}
         |
         |Request: $r with ${r.rawQueryString} ${r.body}""".stripMargin)
    result
  }

  private def badRequest(maybeApiErrorData: Option[APIErrorData], maybeFormErrors: Option[Seq[FormError]], details: JsValue = JsObject.empty)(implicit r: Request[AnyContent]): Result = {
    logAndRespondFor(BadRequest, maybeApiErrorData, maybeFormErrors, details)
  }

  private def notFound(apiErrorData: APIErrorData, details: JsValue = JsObject.empty)(implicit r: Request[AnyContent]): Result = {
    logAndRespondFor(NotFound, Some(apiErrorData), None, details)
  }

  private def invalidTokenRequest[T](details: T = None)(implicit r: Request[AnyContent], tjs: Writes[T]): Result = {
    badRequest(Some(APIErrorData("Invalid token", Some("token"))), None, Json.toJson(details))
  }

  private val actionNameAndTriggerError = "One and only one of actionName and trigger must be set"
  private def checkActionNameAndTrigger(info: ApiMethodWithActionInfo) = {
    (info.actionName.isDefined || info.trigger.isDefined) && (info.actionName.isEmpty || info.trigger.isEmpty)
  }

  private def resultForFormErrors[T <: ApiMethodInfo](formWithErrors: Form[T])(implicit r: Request[AnyContent]): Result = {
    badRequest(None, Some(formWithErrors.errors))
  }

  private val runActionForm = Form(
    mapping(
      "actionName" -> optional(nonEmptyText),
      "trigger" -> optional(nonEmptyText),
      "arguments" -> seq(
        mapping(
          "name" -> nonEmptyText,
          "value" -> nonEmptyText
        )(RunActionArgumentInfo.apply)(RunActionArgumentInfo.unapply)
      ),
      "responseContext" -> nonEmptyText,
      "channel" -> nonEmptyText,
      "token" -> nonEmptyText,
      "originalEventType" -> optional(nonEmptyText)
    )(RunActionInfo.apply)(RunActionInfo.unapply) verifying(actionNameAndTriggerError, checkActionNameAndTrigger _)
  )

  def runAction = Action.async { implicit request =>
    runActionForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(resultForFormErrors(formWithErrors))
      },
      info => {
        val eventualResult = for {
          context <- ApiMethodContextBuilder.createFor(info.token, services)
          result <- info.actionName.map { name =>
            context.runByName(name, info)
          }.getOrElse {
            info.trigger.map { trigger =>
              context.runByTrigger(trigger, info)
            }.getOrElse {
              Future.successful(badRequest(Some(APIErrorData(actionNameAndTriggerError, None)), None, Json.toJson(info)))
            }
          }
        } yield result

        eventualResult.recover {
          case e: InvalidTokenException => invalidTokenRequest(info)
        }
      }
    )

  }

  private val scheduleActionForm = Form(
    mapping(
      "actionName" -> optional(nonEmptyText),
      "trigger" -> optional(nonEmptyText),
      "arguments" -> seq(
        mapping(
          "name" -> nonEmptyText,
          "value" -> nonEmptyText
        )(RunActionArgumentInfo.apply)(RunActionArgumentInfo.unapply)
      ),
      "recurrence" -> nonEmptyText,
      "useDM" -> boolean,
      "channel" -> nonEmptyText,
      "token" -> nonEmptyText
    )(ScheduleActionInfo.apply)(ScheduleActionInfo.unapply) verifying(actionNameAndTriggerError, checkActionNameAndTrigger _)
  )

  def scheduleAction = Action.async { implicit request =>
    scheduleActionForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(resultForFormErrors(formWithErrors))
      },
      info => {
        val eventualResult = for {
          context <- ApiMethodContextBuilder.createFor(info.token, services)
          result <- info.actionName.map { actionName =>
            context.scheduleByName(actionName, info)
          }.getOrElse {
            info.trigger.map { trigger =>
              context.scheduleByTrigger(trigger, info)
            }.getOrElse {
              Future.successful(badRequest(Some(APIErrorData(actionNameAndTriggerError, None)), None, Json.toJson(info)))
            }
          }
        } yield result

        eventualResult.recover {
          case e: InvalidTokenException => invalidTokenRequest(info)
        }
      }
    )

  }

  private val unscheduleActionForm = Form(
    mapping(
      "actionName" -> optional(nonEmptyText),
      "trigger" -> optional(nonEmptyText),
      "userId" -> optional(nonEmptyText),
      "channel" -> optional(nonEmptyText),
      "token" -> nonEmptyText
    )(UnscheduleActionInfo.apply)(UnscheduleActionInfo.unapply) verifying(actionNameAndTriggerError, checkActionNameAndTrigger _)
  )

  def unscheduleAction = Action.async { implicit request =>
    unscheduleActionForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(resultForFormErrors(formWithErrors))
      },
      info => {
        val eventualResult = for {
          context <- ApiMethodContextBuilder.createFor(info.token, services)
          result <- info.actionName.map { actionName =>
            context.unscheduleByName(actionName, info)
          }.getOrElse {
            info.trigger.map { trigger =>
              context.unscheduleByTrigger(trigger, info)
            }.getOrElse {
              Future.successful(badRequest(Some(APIErrorData(actionNameAndTriggerError, None)), None, Json.toJson(info)))
            }
          }
        } yield result

        eventualResult.recover {
          case e: InvalidTokenException => invalidTokenRequest(info)
        }
      }
    )
  }

  private val addMessageListenerForm = Form(
    mapping(
      "actionName" -> nonEmptyText,
      "arguments" -> seq(
        mapping(
          "name" -> nonEmptyText,
          "value" -> nonEmptyText
        )(RunActionArgumentInfo.apply)(RunActionArgumentInfo.unapply)
      ),
      "userId" -> nonEmptyText,
      "medium" -> nonEmptyText,
      "channel" -> nonEmptyText,
      "thread" -> optional(nonEmptyText),
      "token" -> nonEmptyText
    )(AddMessageListenerInfo.apply)(AddMessageListenerInfo.unapply)
  )

  def addMessageListener = Action.async { implicit request =>
    addMessageListenerForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(resultForFormErrors(formWithErrors))
      },
      info => {
        val eventualResult = for {
          context <- ApiMethodContextBuilder.createFor(info.token, services)
          result <- context.addMessageListener(info)
        } yield result

        eventualResult.recover {
          case e: InvalidTokenException => invalidTokenRequest(info)
        }
      }
    )
  }

  private val postMessageForm = Form(
    mapping(
      "message" -> nonEmptyText,
      "responseContext" -> nonEmptyText,
      "channel" -> nonEmptyText,
      "token" -> nonEmptyText,
      "originalEventType" -> optional(nonEmptyText)
    )(PostMessageInfo.apply)(PostMessageInfo.unapply)
  )

  def postMessage = Action.async { implicit request =>
    postMessageForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(resultForFormErrors(formWithErrors))
      },
      info => {
        val eventualResult = for {
          context <- ApiMethodContextBuilder.createFor(info.token, services)
          maybeEvent <- context.maybeMessageEventFor(info.message, info.channel, EventType.maybeFrom(info.originalEventType))
          result <- context.runBehaviorFor(maybeEvent, Right(info.message))
        } yield result

        eventualResult.recover {
          case e: InvalidTokenException => invalidTokenRequest(info)
        }
      }
    )
  }

  private val sayForm = Form(
    mapping(
      "message" -> nonEmptyText,
      "responseContext" -> nonEmptyText,
      "channel" -> nonEmptyText,
      "token" -> nonEmptyText,
      "originalEventType" -> optional(nonEmptyText)
    )(SayInfo.apply)(SayInfo.unapply)
  )

  def say = Action.async { implicit request =>
    sayForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(resultForFormErrors(formWithErrors))
      },
      info => {
        val eventualResult = for {
          context <- ApiMethodContextBuilder.createFor(info.token, services)
          maybeEvent <- context.maybeMessageEventFor(info.message, info.channel, EventType.maybeFrom(info.originalEventType))
          result <- maybeEvent.map { event =>
            val botResult = SimpleTextResult(event, None, info.message, responseType = Normal, shouldInterrupt = false)
            botResultService.sendIn(botResult, None).map { _ =>
              Ok(Json.toJson(Seq(botResult.fullText)))
            }
          }.getOrElse {
            context.printEventCreationError()
            Future.successful(InternalServerError("Request failed.\n"))
          }
        } yield result

        eventualResult.recover {
          case invalidTokenException: InvalidTokenException => invalidTokenRequest(info)
          case channelException: SlackMessageSenderChannelException => {
            badRequest(Some(APIErrorData(s"""Error: ${channelException.rawChannelReason}""", Some("channel"))), None, Json.toJson(info))
          }
          case slackException: SlackMessageSenderException => {
            slackException.underlying match {
              // TODO: 400 seems like maybe the wrong kind of error here
              case apiError: SlackApiError => {
                badRequest(Some(APIErrorData(s"Slack API error: ${apiError.code}\n", None)), None, Json.toJson(info))
              }
              case _ => {
                badRequest(Some(APIErrorData(s"Unknown error while attempting to send message to Slack", None)), None, Json.toJson(info))
              }
            }

          }
        }
      }
    )

  }

  private val generateApiTokenForm = Form(
    mapping(
      "token" -> nonEmptyText,
      "expirySeconds" -> optional(number),
      "isOneTime" -> optional(boolean)
    )(GenerateApiTokenInfo.apply)(GenerateApiTokenInfo.unapply)
  )

  def generateApiToken = Action.async { implicit request =>
    generateApiTokenForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(resultForFormErrors(formWithErrors))
      },
      info => {
        val eventualResult = for {
          context <- ApiMethodContextBuilder.createFor(info.token, services)
          maybeNewToken <- context.maybeInvocationToken.map { invocationToken =>
            dataService.apiTokens.createFor(invocationToken, info.expirySeconds, info.isOneTime.getOrElse(false)).map(Some(_))
          }.getOrElse(Future.successful(None))
        } yield {
          maybeNewToken.map { newToken =>
            Ok(Json.toJson(APITokenData.from(newToken)))
          }.getOrElse {
            Forbidden("Invocation token has expired")
          }
        }

        eventualResult.recover {
          // TODO: look into this and similar cases and maybe do something different
          case e: InvalidTokenException => invalidTokenRequest(info)
        }
      }
    )

  }

  def fetchFile(token: String, fileId: String) = Action.async { implicit request =>
    val eventualResult = for {
      context <- ApiMethodContextBuilder.createFor(token, services)
      result <- context.fetchFileResultFor(fileId)
    } yield result

    eventualResult.recover {
      case e: InvalidTokenException => invalidTokenRequest(Map("token" -> token, "fileId" -> fileId))
    }
  }

  case class FindUsersResult(users: Seq[UserData])

  implicit val findUsersResultWrites = Json.writes[FindUsersResult]

  def findUsers(token: String, maybeEmail: Option[String]) = Action.async { implicit request =>
    val eventualResult = for {
      context <- ApiMethodContextBuilder.createFor(token, services)
      result <- context.maybeTeam.map { team =>
        maybeEmail.map { email =>
          dataService.users.maybeUserDataForEmail(email, team).map { maybeUserData =>
            val users = Seq(maybeUserData).flatten
            Logger.info(s"Sending ${users.length} user data object(s) on team ${team.id} for findUsers API request")
            Ok(Json.toJson(FindUsersResult(users)))
          }
        }.getOrElse {
          Logger.warn(s"findUsers API request with no email param")
          Future.successful(badRequest(Some(APIErrorData("You must pass an `email` parameter to find.", None)), None))
        }
      }.getOrElse {
        Logger.warn(s"findUsers API request with no valid team")
        Future.successful(notFound(APIErrorData("Team not found", None)))
      }
    } yield result

    eventualResult.recover {
      case e: InvalidTokenException => invalidTokenRequest(Map("token" -> token))
    }
  }

  case class DeleteSavedAnswersResult(inputName: String, deletedCount: Int)

  implicit val deleteSavedAnswersResultWrites = Json.writes[DeleteSavedAnswersResult]

  case class DeleteSavedAnswersInfo(inputName: String, deleteAll: Option[Boolean], token: String) extends ApiMethodInfo

  implicit val deleteSavedAnswersInfoWrites = Json.writes[DeleteSavedAnswersInfo]

  private val deleteSavedAnswersForm = Form(
    mapping(
      "inputName" -> nonEmptyText,
      "deleteAll" -> optional(boolean),
      "token" -> nonEmptyText
    )(DeleteSavedAnswersInfo.apply)(DeleteSavedAnswersInfo.unapply)
  )

  private def deleteSavedAnswersFor(deleteSavedAnswersInfo: DeleteSavedAnswersInfo)
                                   (implicit r: Request[AnyContent]): Future[Result] = {
    val token = deleteSavedAnswersInfo.token
    val inputName = deleteSavedAnswersInfo.inputName
    val deleteAll = deleteSavedAnswersInfo.deleteAll.getOrElse(false)
    val eventualResult = for {
      context <- ApiMethodContextBuilder.createFor(token, services)
      maybeBehaviorVersion <- context.maybeOriginatingBehaviorVersion
      savedInputs <- maybeBehaviorVersion.map { behaviorVersion =>
        dataService.inputs.allForGroupVersion(behaviorVersion.groupVersion).map { inputs =>
          inputs.filter(input => input.isSaved && input.name == inputName)
        }
      }.getOrElse(Future.successful(Seq()))
      numDeleted <- {
        for {
          user <- context.maybeUser
        } yield {
          Future.sequence {
            if (deleteAll) {
              savedInputs.map(input => dataService.savedAnswers.deleteAllFor(input.inputId))
            } else {
              savedInputs.map(input => dataService.savedAnswers.deleteForUser(input.inputId, user))
            }
          }
        }
      }.getOrElse(Future.successful(Seq(0))).map(_.sum)
    } yield {
      if (savedInputs.nonEmpty) {
        Ok(Json.toJson(DeleteSavedAnswersResult(inputName, numDeleted)))
      } else {
        NotFound(s"No saved input named `${inputName}` found")
      }
    }
    eventualResult.recover {
      case e: InvalidTokenException => invalidTokenRequest(deleteSavedAnswersInfo)
    }
  }

  def deleteUserSavedAnswer(inputName: String, token: String) = Action.async { implicit request =>
    deleteSavedAnswersFor(DeleteSavedAnswersInfo(inputName, Some(false), token))
  }

  def deleteTeamSavedAnswers(inputName: String, token: String) = Action.async { implicit request =>
    deleteSavedAnswersFor(DeleteSavedAnswersInfo(inputName, Some(true), token))
  }

  def deleteSavedAnswers = Action.async { implicit request =>
    deleteSavedAnswersForm.bindFromRequest.fold(
      formWithErrors => Future.successful(resultForFormErrors(formWithErrors)),
      info => deleteSavedAnswersFor(info)
    )
  }
}
