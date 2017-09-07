package controllers

import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

import akka.actor.ActorSystem
import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import json.Formatting._
import json._
import models.accounts.user.User
import models.behaviors.scheduling.scheduledbehavior.ScheduledBehavior
import models.behaviors.scheduling.scheduledmessage.ScheduledMessage
import models.silhouette.EllipsisEnv
import models.team.Team
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.filters.csrf.CSRF
import services.{CacheService, DataService}

import scala.concurrent.{ExecutionContext, Future}


class ScheduledActionsController @Inject()(
                                            val configuration: Configuration,
                                            val silhouette: Silhouette[EllipsisEnv],
                                            val dataService: DataService,
                                            val cacheService: CacheService,
                                            val assetsProvider: Provider[RemoteAssets],
                                            implicit val actorSystem: ActorSystem,
                                            implicit val ec: ExecutionContext
                                          ) extends ReAuthable {

  def index(maybeScheduledId: Option[String], maybeNewSchedule: Option[Boolean], maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity

    render.async {
      case Accepts.JavaScript() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
          result <- teamAccess.maybeTargetTeam.map { team =>
            for {
              maybeBotProfile <- dataService.slackBotProfiles.allFor(team).map(_.headOption)
              maybeSlackUserId <- dataService.linkedAccounts.maybeSlackUserIdFor(user)
              channelList <- maybeBotProfile.map { botProfile =>
                dataService.slackBotProfiles.channelsFor(botProfile, cacheService).getListForUser(maybeSlackUserId)
              }.getOrElse(Future.successful(Seq()))
              scheduledActions <- ScheduledActionData.buildFor(maybeSlackUserId, team, channelList, dataService)
              behaviorGroups <- dataService.behaviorGroups.allFor(team)
              groupData <- Future.sequence(behaviorGroups.map { group =>
                BehaviorGroupData.maybeFor(group.id, user, None, dataService)
              }).map(_.flatten.sorted)
            } yield {
              val pageData = ScheduledActionsConfig(
                containerId = "scheduling",
                csrfToken = CSRF.getToken(request).map(_.value),
                teamId = team.id,
                scheduledActions = scheduledActions,
                channelList = ScheduleChannelData.fromChannelLikeList(channelList),
                behaviorGroups = groupData,
                teamTimeZone = team.maybeTimeZone.map(_.toString),
                teamTimeZoneName = team.maybeTimeZone.map(_.getDisplayName(TextStyle.FULL, Locale.ENGLISH)),
                slackUserId = maybeSlackUserId,
                slackBotUserId = maybeBotProfile.map(_.userId),
                selectedScheduleId = maybeScheduledId,
                newAction = maybeNewSchedule
              )
              Ok(views.js.shared
                .pageConfig(viewConfig(Some(teamAccess)), "config/scheduling/index", Json.toJson(pageData))
              )
            }
          }.getOrElse {
            Future.successful(NotFound("Team not found"))
          }
        } yield result
      }
      case Accepts.Html() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
        } yield {
          teamAccess.maybeTargetTeam.map { _ =>
            Ok(views.html.scheduledactions.index(viewConfig(Some(teamAccess)), maybeScheduledId, maybeNewSchedule, maybeTeamId))
          }.getOrElse {
            NotFound("Team not found")
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
        val newArguments = newData.arguments.map(ea => ea.name -> ea.value).toMap
        val maybeChannel = Option(newData.channel).filter(_.trim.nonEmpty)
        dataService.scheduledBehaviors.createFor(behavior, newArguments, recurrence, user, team, maybeChannel, newData.useDM).map(Some(_))
      }).getOrElse(Future.successful(None))
    } yield {
      if (maybeNewScheduledBehavior.isDefined) {
        maybeOriginal.map { original =>
          dataService.scheduledBehaviors.delete(original)
        }
      }
      maybeNewScheduledBehavior
    }
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
          dataService.scheduledMessages.createFor(trigger, recurrence, user, team, maybeChannel, newData.useDM).map(Some(_))
        }.getOrElse(Future.successful(None))
      } yield {
        if (maybeNewScheduledMessage.isDefined) {
          maybeOriginal.map { original =>
            dataService.scheduledMessages.delete(original)
          }
        }
        maybeNewScheduledMessage
      }
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
        dataService.scheduledMessages.delete(scheduledMessage)
      }.getOrElse(Future.successful(false))
    } yield didDeleteMessage
  }

  private def maybeDeleteScheduledBehavior(id: String, team: Team): Future[Boolean] = {
    for {
      maybeExistingScheduledBehavior <- dataService.scheduledBehaviors.findForTeam(id, team)
      didDeleteBehavior <- maybeExistingScheduledBehavior.map { scheduledBehavior =>
        dataService.scheduledBehaviors.delete(scheduledBehavior)
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
}
