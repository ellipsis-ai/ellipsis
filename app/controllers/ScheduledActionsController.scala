package controllers

import java.time.OffsetDateTime

import javax.inject.Inject
import akka.actor.ActorSystem
import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import json.Formatting._
import json._
import models.accounts.user.User
import models.behaviors.scheduling.recurrence.Recurrence
import models.behaviors.scheduling.scheduledbehavior.ScheduledBehavior
import models.behaviors.scheduling.scheduledmessage.ScheduledMessage
import models.silhouette.EllipsisEnv
import models.team.Team
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.filters.csrf.CSRF
import services.DefaultServices

import scala.concurrent.{ExecutionContext, Future}


class ScheduledActionsController @Inject()(
                                            val configuration: Configuration,
                                            val silhouette: Silhouette[EllipsisEnv],
                                            val services: DefaultServices,
                                            val assetsProvider: Provider[RemoteAssets],
                                            implicit val actorSystem: ActorSystem,
                                            implicit val ec: ExecutionContext
                                          ) extends ReAuthable {

  val dataService = services.dataService

  def index(
             maybeScheduledId: Option[String],
             maybeNewSchedule: Option[Boolean],
             maybeChannelId: Option[String],
             maybeTeamId: Option[String],
             maybeForceAdmin: Option[Boolean]
           ) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity

    render.async {
      case Accepts.JavaScript() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
          forceAdmin <- dataService.users.isAdmin(user).map { userIsAdmin =>
            userIsAdmin && maybeForceAdmin.contains(true)
          }
          maybeConfig <- ScheduledActionsConfig.buildConfigFor(
            user = user,
            teamAccess = teamAccess,
            services = services,
            maybeScheduledId = maybeScheduledId,
            maybeNewSchedule = maybeNewSchedule,
            maybeFilterChannelId = maybeChannelId,
            maybeCsrfToken = CSRF.getToken(request).map(_.value),
            forceAdmin = forceAdmin
          )
        } yield {
          maybeConfig.map { config =>
            Ok(views.js.shared.webpackLoader(
              viewConfig(Some(teamAccess)),
              "SchedulingConfig",
              "scheduling",
              Json.toJson(config)
            ))
          }.getOrElse {
            NotFound("Team not found")
          }
        }
      }
      case Accepts.Html() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
        } yield {
          teamAccess.maybeTargetTeam.map { _ =>
            Ok(views.html.scheduledactions.index(
              viewConfig(Some(teamAccess)),
              maybeScheduledId,
              maybeNewSchedule,
              maybeChannelId,
              maybeTeamId,
              maybeForceAdmin
            ))
          }.getOrElse {
            NotFound(views.html.error.notFound(viewConfig(None), Some("Team not found"), None))
          }
        }
      }
    }
  }

  trait ScheduledActionForm {
    val scheduleType: String
    val teamId: String
  }

  case class ScheduledActionSaveForm(dataJson: String, scheduleType: String, teamId: String) extends ScheduledActionForm
  case class ScheduledActionDeleteForm(id: String, scheduleType: String, teamId: String) extends ScheduledActionForm

  private val scheduleTypeError: String = "Schedule type must be message or behavior"
  private def validScheduleType(info: ScheduledActionForm) = {
    info.scheduleType == "message" || info.scheduleType == "behavior"
  }

  private val saveForm = Form(
    mapping(
      "dataJson" -> nonEmptyText,
      "scheduleType" -> nonEmptyText,
      "teamId" -> nonEmptyText
    )(ScheduledActionSaveForm.apply)(ScheduledActionSaveForm.unapply) verifying(scheduleTypeError, validScheduleType _)
  )

  private val deleteForm = Form(
    mapping(
      "id" -> nonEmptyText,
      "scheduleType" -> nonEmptyText,
      "teamId" -> nonEmptyText
    )(ScheduledActionDeleteForm.apply)(ScheduledActionDeleteForm.unapply) verifying(scheduleTypeError, validScheduleType _)
  )

  private def maybeNewScheduledBehavior(
                                            maybeOriginal: Option[ScheduledBehavior],
                                            newData: ScheduledActionData,
                                            user: User,
                                            team: Team
                                          ): Future[Option[ScheduledBehavior]] = {
    for {
      maybeBehavior <- newData.behaviorId.map { behaviorId =>
        maybeOriginal.filter(original => original.behavior.id == behaviorId).map { original =>
          Future.successful(Some(original.behavior))
        }.getOrElse {
          dataService.behaviors.find(behaviorId, user)
        }
      }.getOrElse(Future.successful(None))
      maybeNewRecurrence <- newData.recurrence.maybeNewRecurrence.map { recurrence =>
        dataService.recurrences.save(recurrence).map(Some(_))
      }.getOrElse(Future.successful(None))
      maybeNewScheduledBehavior <- (for {
        behavior <- maybeBehavior
        recurrence <- maybeNewRecurrence
      } yield {
        val maybeChannel = Option(newData.channel).filter(_.trim.nonEmpty)
        maybeOriginal.map { original =>
          dataService.scheduledBehaviors.save(original.copy(
            behavior = behavior,
            arguments = newData.arguments,
            recurrence = recurrence,
            nextSentAt = recurrence.nextAfter(OffsetDateTime.now),
            maybeChannel = maybeChannel,
            isForIndividualMembers = newData.useDM
          ))
        }.getOrElse {
          dataService.scheduledBehaviors.createFor(behavior, newData.arguments, recurrence, user, team, maybeChannel, newData.useDM)
        }.map(Some(_))
      }).getOrElse(Future.successful(None))
    } yield maybeNewScheduledBehavior
  }

  private def maybeNewScheduledMessage(
                                         maybeOriginal: Option[ScheduledMessage],
                                         newData: ScheduledActionData,
                                         user: User,
                                         team: Team
                                         ): Future[Option[ScheduledMessage]] = {
    newData.trigger.filter(_.nonEmpty).map { trigger =>
      for {
        maybeNewRecurrence <- newData.recurrence.maybeNewRecurrence.map { recurrence =>
          dataService.recurrences.save(recurrence).map(Some(_))
        }.getOrElse(Future.successful(None))
        maybeNewScheduledMessage <- maybeNewRecurrence.map { recurrence =>
          val maybeChannel = Option(newData.channel).filter(_.trim.nonEmpty)
          maybeOriginal.map { original =>
            dataService.scheduledMessages.save(original.copy(
              text = trigger,
              recurrence = recurrence,
              nextSentAt = recurrence.nextAfter(OffsetDateTime.now),
              maybeChannel = maybeChannel,
              isForIndividualMembers = newData.useDM
            ))
          }.getOrElse {
            dataService.scheduledMessages.createFor(trigger, recurrence, user, team, maybeChannel, newData.useDM)
          }.map(Some(_))
        }.getOrElse(Future.successful(None))
      } yield maybeNewScheduledMessage
    }.getOrElse(Future.successful(None))
  }

  private def maybeNewScheduledMessageData(data: ScheduledActionData, user: User, team: Team): Future[Option[ScheduledActionData]] = {
    for {
      maybeExistingScheduledMessage <- data.id.map { scheduleId =>
        dataService.scheduledMessages.findForTeam(scheduleId, team)
      }.getOrElse(Future.successful(None))
      maybeNewScheduledMessage <- maybeExistingScheduledMessage.map { existingScheduled =>
        maybeNewScheduledMessage(Some(existingScheduled), data, user, team)
      }.getOrElse {
        if (data.id.isEmpty) {
          maybeNewScheduledMessage(None, data, user, team)
        } else {
          Future.successful(None)
        }
      }
    } yield {
      maybeNewScheduledMessage.map(ScheduledActionData.fromScheduledMessage)
    }
  }

  private def maybeNewScheduledBehaviorData(data: ScheduledActionData, user: User, team: Team): Future[Option[ScheduledActionData]] = {
    for {
      maybeExistingScheduledBehavior <- data.id.map { scheduleId =>
        dataService.scheduledBehaviors.findForTeam(scheduleId, team)
      }.getOrElse(Future.successful(None))
      maybeNewScheduledBehavior <- maybeExistingScheduledBehavior.map { existingScheduled =>
        maybeNewScheduledBehavior(Some(existingScheduled), data, user, team)
      }.getOrElse {
        if (data.id.isEmpty) {
          maybeNewScheduledBehavior(None, data, user, team)
        } else {
          Future.successful(None)
        }
      }
    } yield {
      maybeNewScheduledBehavior.map(ScheduledActionData.fromScheduledBehavior)
    }
  }

  def save = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    saveForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        val json = Json.parse(info.dataJson)
        json.validate[ScheduledActionData] match {
          case JsSuccess(data, jsPath) => {
            for {
              teamAccess <- dataService.users.teamAccessFor(user, Some(info.teamId))
              result <- teamAccess.maybeTargetTeam.map { team =>
                for {
                  maybeNewScheduledActionData <- if (info.scheduleType == "message") {
                    maybeNewScheduledMessageData(data, user, team)
                  } else {
                    maybeNewScheduledBehaviorData(data, user, team)
                  }
                } yield {
                  maybeNewScheduledActionData.map { data =>
                    Ok(Json.toJson(data))
                  }.getOrElse {
                    NotFound(Json.toJson("Scheduled action not found"))
                  }
                }
              }.getOrElse {
                Future.successful(NotFound(Json.toJson("Team not found")))
              }
            } yield result
          }
          case e: JsError => {
            Future.successful(BadRequest(Json.toJson(s"Malformatted data: ${e.errors.mkString("\n")}")))
          }
        }
      }
    )
  }

  private def maybeDeleteScheduledMessage(id: String, team: Team): Future[Boolean] = {
    for {
      maybeExistingScheduledMessage <- dataService.scheduledMessages.findForTeam(id, team)
      didDeleteMessage <- maybeExistingScheduledMessage.map { scheduledMessage =>
        dataService.scheduledMessages.delete(scheduledMessage).map(_.isDefined)
      }.getOrElse(Future.successful(false))
    } yield didDeleteMessage
  }

  private def maybeDeleteScheduledBehavior(id: String, team: Team): Future[Boolean] = {
    for {
      maybeExistingScheduledBehavior <- dataService.scheduledBehaviors.findForTeam(id, team)
      didDeleteBehavior <- maybeExistingScheduledBehavior.map { scheduledBehavior =>
        dataService.scheduledBehaviors.delete(scheduledBehavior).map(_.isDefined)
      }.getOrElse(Future.successful(false))
    } yield didDeleteBehavior
  }

  def delete = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    deleteForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, Some(info.teamId))
          result <- teamAccess.maybeTargetTeam.map { team =>
            for {
              deleted <- if (info.scheduleType == "message") {
                maybeDeleteScheduledMessage(info.id, team)
              } else {
                maybeDeleteScheduledBehavior(info.id, team)
              }
            } yield {
              if (deleted) {
                Ok(Json.toJson(Json.obj("deletedId" -> info.id)))
              } else {
                NotFound(Json.toJson("Scheduled action not found"))
              }
            }
          }.getOrElse {
            Future.successful(NotFound(Json.toJson("Team not found")))
          }
        } yield result
      }
    )
  }

  def validateRecurrence = silhouette.SecuredAction(parse.json) { implicit request =>
    request.body.validate[ScheduledActionRecurrenceData].fold(
      jsonError => {
        BadRequest(JsError.toJson(jsonError))
      },
      recurrenceData => {
        recurrenceData.maybeNewRecurrence.map { recurrence =>
          val now = OffsetDateTime.now
          val maybeRemainingRuns = recurrence.maybeTotalTimesToRun.map { totalTimes =>
            math.max(0, totalTimes - recurrence.timesHasRun)
          }
          val maybeFirst = if (maybeRemainingRuns.isEmpty || maybeRemainingRuns.exists(_ > 0)) {
            Some(recurrence.nextAfter(now))
          } else {
            None
          }
          val maybeSecond = if (maybeRemainingRuns.isEmpty || maybeRemainingRuns.exists(_ > 1)) {
            maybeFirst.map(recurrence.nextAfter)
          } else {
            None
          }
          Ok(Json.toJson(
            ScheduledActionValidatedRecurrenceData(
              ScheduledActionRecurrenceData.fromRecurrence(recurrence),
              Seq(maybeFirst, maybeSecond)
            )
          ))
        }.getOrElse {
          BadRequest("No valid recurrence was found.")
        }
      }
    )
  }
}
