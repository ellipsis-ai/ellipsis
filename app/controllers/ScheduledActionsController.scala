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
              scheduledMessageData <- ScheduledActionData.fromScheduledMessages(scheduledMessages, channelList)
              scheduledBehaviorData <- ScheduledActionData
                .fromScheduledBehaviors(scheduledBehaviors, dataService, channelList)
              maybeSlackLinkedAccount <- dataService.linkedAccounts.maybeForSlackFor(user)
              maybeSlackProfile <- maybeSlackLinkedAccount.map { linkedAccount =>
                dataService.slackProfiles.find(linkedAccount.loginInfo)
              }.getOrElse(Future.successful(None))
            } yield {
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
                                            scheduledBehavior: ScheduledBehavior,
                                            newData: ScheduledActionData,
                                            user: User,
                                            team: Team
                                          ): Future[Option[ScheduledBehavior]] = {
    for {
      maybeBehavior <- newData.behaviorId.map { behaviorId =>
        if (behaviorId == scheduledBehavior.behavior.id) {
          Future.successful(Some(scheduledBehavior.behavior))
        } else {
          dataService.behaviors.find(behaviorId, user)
        }
      }.getOrElse(Future.successful(None))
      maybeRecurrence <- newData.recurrence.maybeNewRecurrence.map { recurrence =>
        dataService.recurrences.delete(scheduledBehavior.recurrence.id)
        dataService.recurrences.save(recurrence).map(Some(_))
      }.getOrElse(Future.successful(None))
      maybeScheduledBehavior <- (for {
        behavior <- maybeBehavior
        recurrence <- maybeRecurrence
      } yield {
        val newArguments = newData.arguments.map(ea => ea.name -> ea.value).toMap
        val maybeChannel = Option(newData.channel).filter(_.trim.nonEmpty)
        dataService.scheduledBehaviors.delete(scheduledBehavior)
        dataService.scheduledBehaviors.createFor(behavior, newArguments, recurrence, user, team, maybeChannel, newData.useDM).map(Some(_))
      }).getOrElse(Future.successful(None))
    } yield maybeScheduledBehavior
  }

  private def maybeUpdateScheduledMessage(
                                         scheduledMessage: ScheduledMessage,
                                         newData: ScheduledActionData,
                                         user: User,
                                         team: Team
                                         ): Future[Option[ScheduledMessage]] = {
    newData.trigger.filter(_.nonEmpty).map { trigger =>
      for {
        maybeRecurrence <- newData.recurrence.maybeNewRecurrence.map { recurrence =>
          dataService.recurrences.delete(scheduledMessage.recurrence.id)
          dataService.recurrences.save(recurrence).map(Some(_))
        }.getOrElse(Future.successful(None))
        maybeScheduledMessage <- maybeRecurrence.map { recurrence =>
          val maybeChannel = Option(newData.channel).filter(_.trim.nonEmpty)
          dataService.scheduledMessages.deleteFor(scheduledMessage.text, team)
          dataService.scheduledMessages.createFor(trigger, recurrence, user, team, maybeChannel, newData.useDM).map(Some(_))
        }.getOrElse(Future.successful(None))
      } yield maybeScheduledMessage
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
                  maybeBotProfile <- dataService.slackBotProfiles.allFor(team).map(_.headOption)
                  channelList <- maybeBotProfile.map { botProfile =>
                    dataService.slackBotProfiles.channelsFor(botProfile).listInfos
                  }.getOrElse(Future.successful(Seq()))
                  maybeExistingScheduledBehavior <- dataService.scheduledBehaviors.find(data.id).map { maybeScheduled =>
                    maybeScheduled.filter(_.team.id == team.id)
                  }
                  maybeExistingScheduledMessage <- dataService.scheduledMessages.find(data.id).map { maybeScheduled =>
                    maybeScheduled.filter(_.team.id == team.id)
                  }
                  maybeUpdatedScheduledBehavior <- maybeExistingScheduledBehavior.map { existingScheduled =>
                    maybeUpdateScheduledBehavior(existingScheduled, data, user, team)
                  }.getOrElse(Future.successful(None))
                  maybeUpdatedScheduledMessage <- maybeExistingScheduledMessage.map { existingScheduled =>
                    maybeUpdateScheduledMessage(existingScheduled, data, user, team)
                  }.getOrElse(Future.successful(None))
                  behaviorData <- maybeUpdatedScheduledBehavior.map { scheduledBehavior =>
                    ScheduledActionData.fromScheduledBehaviors(Seq(scheduledBehavior), dataService, channelList)
                  }.getOrElse(Future.successful(Seq()))
                  messageData <- maybeUpdatedScheduledMessage.map { scheduledMessage =>
                    ScheduledActionData.fromScheduledMessages(Seq(scheduledMessage), channelList)
                  }.getOrElse(Future.successful(Seq()))
                } yield {
                  (behaviorData ++ messageData).headOption.map { scheduledData =>
                    Ok(Json.toJson(scheduledData))
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
