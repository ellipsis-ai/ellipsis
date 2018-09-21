package models.behaviors.builtins

import akka.actor.ActorSystem
import json.UserData
import models.accounts.linkedaccount.LinkedAccount
import models.accounts.user.User
import models.behaviors.behaviorversion.{Normal, Private}
import models.behaviors.events.{Event, EventType}
import models.behaviors.{BotResult, SimpleTextResult}
import models.team.Team
import play.api.{Configuration, Logger}
import services.slack.SlackApiError
import services.{DataService, DefaultServices}

import scala.concurrent.{ExecutionContext, Future}

case class FeedbackBehavior(feedbackType: String, userMessage: String, event: Event, services: DefaultServices) extends BuiltinBehavior {
  val dataService: DataService = services.dataService
  val configuration: Configuration = services.configuration

  def result(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    for {
      user <- event.ensureUser(dataService)
      maybeTeam <- dataService.teams.find(user.teamId)
    } yield {
      maybeTeam.map { team =>
        FeedbackBehavior.feedbackFor(user, team, services, feedbackType, userMessage, EventType.chat)
      }
      val response =
        s"""Thank you. I’ve recorded your comments and sent it to the team at Ellipsis.ai:
           |
           |${userMessage.lines.mkString("> ", "\n", "")}
         """.stripMargin
      SimpleTextResult(event, None, response, responseType = Private)
    }
  }
}

object FeedbackBehavior {
  def feedbackFor(user: User, team: Team, services: DefaultServices, feedbackType: String, userMessage: String, originalEventType: EventType)
                 (implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Boolean] = {
    for {
      userData <- services.dataService.users.userDataFor(user, team)
      wasSent <- sendFeedbackToAdminTeam(
        feedbackMessage(teamInfo(team, services), userInfo(user.id, Some(userData)), feedbackType, userMessage),
        services,
        originalEventType
      )
    } yield wasSent
  }

  def supportRequest(maybeUser: Option[User], maybeTeam: Option[Team], services: DefaultServices, name: String, emailAddress: String, message: String)
                    (implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Boolean] = {
    for {
      maybeUserData <- (for {
        user <- maybeUser
        team <- maybeTeam
      } yield {
        services.dataService.users.userDataFor(user, team).map(Some(_))
      }).getOrElse(Future.successful(None))
      wasSent <- {
        val maybeTeamInfo = maybeTeam.map(team => teamInfo(team, services))
        val maybeUserInfo = maybeUser.map(user => userInfo(user.id, maybeUserData))
        sendFeedbackToAdminTeam(
          supportMessage(maybeTeamInfo, maybeUserInfo, name, emailAddress, message),
          services,
          EventType.web
        )
      }
    } yield wasSent
  }

  private def sendFeedbackToAdminTeam(msg: String, services: DefaultServices, originalEventType: EventType)
                                     (implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Boolean] = {
    for {
      maybeAdminTeamEvent <- services.dataService.slackBotProfiles.eventualMaybeEvent(
        LinkedAccount.ELLIPSIS_SLACK_TEAM_ID,
        LinkedAccount.ELLIPSIS_SLACK_FEEDBACK_CHANNEL_ID,
        None,
        Some(originalEventType)
      )
      wasSent <- maybeAdminTeamEvent.map { adminTeamEvent =>
        val result = SimpleTextResult(adminTeamEvent, None, msg, responseType = Normal)
        services.botResultService.sendIn(result, None).map(_.isDefined).recover {
          case e: SlackApiError => {
            false
          }
        }
      }.getOrElse(Future.successful(false))
    } yield {
      if (wasSent) {
        Logger.info(s"User feedback sent: $msg")
      } else {
        Logger.error(s"User feedback failed to send: $msg")
      }
      wasSent
    }
  }

  private def userInfo(userId: String, maybeUserData: Option[UserData]): String = {
    maybeUserData.map { userData =>
      val fullName = userData.fullName.getOrElse("Unknown user")
      val userName = userData.userName.map(userName => s"@$userName").getOrElse("unknown username")
      val email = userData.email.getOrElse("(unknown)")
      val tz = userData.tz.getOrElse("(unknown)")
      s"""> Name: ${fullName} (${userName})
         |> Email: ${email}
         |> Time zone: ${tz}
         |> Ellipsis ID: ${userId}""".stripMargin
    }.getOrElse(s"User #$userId")
  }

  private def teamInfo(team: Team, services: DefaultServices): String = {
    val maybeTeamUrl = services.configuration.getOptional[String]("application.apiBaseUrl").map { baseUrl =>
      baseUrl + controllers.routes.ApplicationController.index(Some(team.id))
    }
    maybeTeamUrl.map { url =>
      s"**[${team.name}]($url)**"
    }.getOrElse {
      s"**${team.name} (#${team.id})"
    }
  }

  private def feedbackMessage(teamInfo: String, userInfo: String, feedbackType: String, message: String): String = {
    s"""${feedbackType}:
       |
       |**User:**
       |$userInfo
       |Team: $teamInfo
       |
       |**Message:**
       |${message.lines.mkString("> ", "\n> ", "\n")}
     """.stripMargin
  }

  private def supportMessage(maybeTeamInfo: Option[String], maybeUserInfo: Option[String], name: String, emailAddress: String, message: String): String = {
    val teamInfo = maybeTeamInfo.getOrElse("None")
    val userInfo = maybeUserInfo.getOrElse("None")
    val feedbackType = s":rotating_light: Support request from **$name** · <$emailAddress>"
    feedbackMessage(teamInfo, userInfo, feedbackType, message)
  }
}
