package controllers

import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

import akka.actor.ActorSystem
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
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.filters.csrf.CSRF
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class ScheduledActionsController @Inject()(
                                            val messagesApi: MessagesApi,
                                            val configuration: Configuration,
                                            val silhouette: Silhouette[EllipsisEnv],
                                            val dataService: DataService,
                                            implicit val actorSystem: ActorSystem
                                          ) extends ReAuthable {

  def index(maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity

    render.async {
      case Accepts.JavaScript() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
          result <- teamAccess.maybeTargetTeam.map { team =>
            for {
              scheduledMessages <- dataService.scheduledMessages.allForTeam(team)
              scheduledBehaviors <- dataService.scheduledBehaviors.allForTeam(team)
              maybeBotProfile <- dataService.slackBotProfiles.allFor(team).map(_.headOption)
              channelList <- maybeBotProfile.map { botProfile =>
                dataService.slackBotProfiles.channelsFor(botProfile).listInfos
              }.getOrElse(Future.successful(Seq()))
              behaviorGroups <- dataService.behaviorGroups.allFor(team)
              groupData <- Future.sequence(behaviorGroups.map { group =>
                BehaviorGroupData.maybeFor(group.id, user, None, dataService)
              }
              ).map(_.flatten.sorted)
              maybeSlackLinkedAccount <- dataService.linkedAccounts.maybeForSlackFor(user)
              maybeSlackProfile <- maybeSlackLinkedAccount.map { linkedAccount =>
                dataService.slackProfiles.find(linkedAccount.loginInfo)
              }.getOrElse(Future.successful(None))
            } yield {
              val scheduledMessageData = scheduledMessages.map(ScheduledActionData.fromScheduledMessage)
              val scheduledBehaviorData = scheduledBehaviors.map(ScheduledActionData.fromScheduledBehavior)
              val pageData = ScheduledActionsConfig(
                containerId = "scheduling",
                csrfToken = CSRF.getToken(request).map(_.value),
                teamId = team.id,
                scheduledActions = scheduledMessageData ++ scheduledBehaviorData,
                channelList = ScheduleChannelData.fromChannelLikeList(channelList),
                behaviorGroups = groupData,
                teamTimeZone = team.maybeTimeZone.map(_.toString),
                teamTimeZoneName = team.maybeTimeZone.map(_.getDisplayName(TextStyle.FULL, Locale.ENGLISH)),
                slackUserId = maybeSlackProfile.map(_.loginInfo.providerKey)
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
            Ok(views.html.scheduledactions.index(viewConfig(Some(teamAccess)), maybeTeamId))
          }.getOrElse {
            NotFound("Team not found")
          }
        }
      }
    }
  }

  case class ScheduledActionSaveForm(dataJson: String, teamId: String)

  private val saveForm = Form(
    mapping(
      "dataJson" -> nonEmptyText,
      "teamId" -> nonEmptyText
    )(ScheduledActionSaveForm.apply)(ScheduledActionSaveForm.unapply)
  )

  private def maybeUpdateScheduledBehavior(
                                            original: ScheduledBehavior,
                                            newData: ScheduledActionData,
                                            user: User,
                                            team: Team
                                          ): Future[Option[ScheduledBehavior]] = {
    for {
      maybeBehavior <- newData.behaviorId.map { behaviorId =>
        if (behaviorId == original.behavior.id) {
          Future.successful(Some(original.behavior))
        } else {
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
        dataService.scheduledBehaviors.delete(original)
      }
      maybeNewScheduledBehavior
    }
  }

  private def maybeUpdateScheduledMessage(
                                         original: ScheduledMessage,
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
          dataService.scheduledMessages.delete(original)
        }
        maybeNewScheduledMessage
      }
    }.getOrElse(Future.successful(None))
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
                  maybeUpdatedData <- if (data.scheduleType == "message") {
                    for {
                      maybeExistingScheduledMessage <- dataService.scheduledMessages.findForTeam(data.id, team)
                      maybeUpdatedScheduledMessage <- maybeExistingScheduledMessage.map { existingScheduled =>
                        maybeUpdateScheduledMessage(existingScheduled, data, user, team)
                      }.getOrElse(Future.successful(None))
                    } yield {
                      maybeUpdatedScheduledMessage.map(ScheduledActionData.fromScheduledMessage)
                    }
                  } else if (data.scheduleType == "behavior") {
                    for {
                      maybeExistingScheduledBehavior <- dataService.scheduledBehaviors.findForTeam(data.id, team)
                      maybeUpdatedScheduledBehavior <- maybeExistingScheduledBehavior.map { existingScheduled =>
                        maybeUpdateScheduledBehavior(existingScheduled, data, user, team)
                      }.getOrElse(Future.successful(None))
                    } yield {
                      maybeUpdatedScheduledBehavior.map(ScheduledActionData.fromScheduledBehavior)
                    }
                  } else {
                    Future.successful(None)
                  }
                } yield {
                  maybeUpdatedData.map { data =>
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
}
