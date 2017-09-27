package models.behaviors.builtins

import akka.actor.ActorSystem
import json.UserData
import models.accounts.linkedaccount.LinkedAccount
import models.behaviors.events.Event
import models.behaviors.{BotResult, SimpleTextResult}
import play.api.Logger
import services.{DataService, DefaultServices}

import scala.concurrent.{ExecutionContext, Future}

case class FeedbackBehavior(event: Event, services: DefaultServices) extends BuiltinBehavior {
  val dataService: DataService = services.dataService

  def result(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    for {
      user <- event.ensureUser(dataService)
      maybeTeam <- dataService.teams.find(user.teamId)
      maybeUserData <- maybeTeam.map { team =>
        dataService.users.userDataFor(user, team).map(Some(_))
      }.getOrElse(Future.successful(None))
    } yield {
      val msg = ":tada:"
      maybeTeam.map { team =>
        sendFeedbackToAdminTeam(Feedback(team.id, team.name, user.id, maybeUserData).message(msg))
      }
      SimpleTextResult(event, None, "Thank you. Your feedback has been received.", forcePrivateResponse = true)
    }
  }

  private def sendFeedbackToAdminTeam(msg: String)(implicit actorSystem: ActorSystem, ec: ExecutionContext) = {
    for {
      maybeAdminTeamEvent <- dataService.slackBotProfiles.eventualMaybeEvent(LinkedAccount.ELLIPSIS_SLACK_TEAM_ID, LinkedAccount.ELLIPSIS_FEEDBACK_CHANNEL, None)
      wasSent <- maybeAdminTeamEvent.map { adminTeamEvent =>
        val result = SimpleTextResult(adminTeamEvent, None, msg, forcePrivateResponse = false)
        services.botResultService.sendIn(result, None).map(_.isDefined)
      }.getOrElse(Future.successful(false))
    } yield {
      if (wasSent) {
        Logger.info(s"User feedback sent: $msg")
      } else {
        Logger.error(s"User feedback failed to send: $msg")
      }
    }
  }

}

case class Feedback(teamId: String, teamName: String, userId: String, maybeUserData: Option[UserData]) {
  def userInfo: String = {
    maybeUserData.map { userData =>
      s"""**${userData.fullName.getOrElse("Unknown")}** ${userData.userName.map(userName => s"@$userName").getOrElse("(unknown username)")} (#$userId)"""
    }.getOrElse(s"User #$userId")
  }

  def message(msg: String): String = {
    s"""Feedback:
       |
       |👤 $userInfo
       |💼 **$teamName** (#$teamId)
       |
       |${msg.lines.mkString("> ", "\n", "")}
     """.stripMargin
  }
}

