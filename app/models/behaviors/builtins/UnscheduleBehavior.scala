package models.behaviors.builtins

import akka.actor.ActorSystem
import json.{BehaviorGroupData, BehaviorTriggerData, BehaviorVersionData}
import models.behaviors.events.Event
import models.behaviors.{BotResult, SimpleTextResult}
import models.team.Team
import services.{DataService, DefaultServices}

import scala.concurrent.{ExecutionContext, Future}

case class UnscheduleBehavior(
                               text: String,
                               event: Event,
                               services: DefaultServices
                             ) extends BuiltinBehavior {

  def result(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    val configuration = services.configuration
    val dataService = services.dataService
    for {
      maybeTeam <- dataService.teams.find(event.teamId)
      didDelete <- maybeTeam.map { team =>
        dataService.scheduledMessages.deleteFor(text, team)
      }.getOrElse(Future.successful(false))
      scheduled <- maybeTeam.map { team =>
        dataService.scheduledMessages.allForTeam(team)
      }.getOrElse(Future.successful(Seq()))
      listResponses <- Future.sequence(scheduled.map(ea => ea.listResponse(ea.id, ea.team.id, dataService, configuration, includeChannel = true)))
    } yield {
      val msg = if (didDelete) {
        s"OK, I unscheduled `$text`"
      } else {
        val alternativesMessage = if(scheduled.isEmpty) {
          "You don’t currently have anything scheduled."
        } else {
          s"Here’s what you have scheduled currently:\n\n${listResponses.mkString}"
        }
        s"I couldn't find `$text` scheduled. $alternativesMessage"
      }
      SimpleTextResult(event, None, msg, forcePrivateResponse = false)
    }
  }

}

object UnscheduleBehavior extends BuiltinImplementationType {

  val builtinId: String = "unschedule"
  val name: String = "Unschedule an action"

  def behaviorVersionsDataFor(team: Team, dataService: DataService) = Seq(
    BehaviorVersionData.newUnsavedFor(team.id, isDataType = false, Some(name), dataService).copy(
      triggers = Seq(BehaviorTriggerData(s"""(?i)^unschedule\\s+([`"'])(.*?)\\1\\s*$$""", requiresMention = true, isRegex = true, caseSensitive = false)),
      builtinName = Some(builtinId)
    )
  )

  def addToGroupDataTo(data: BehaviorGroupData, team: Team, dataService: DataService): BehaviorGroupData = {
    data.copy(
      behaviorVersions = data.behaviorVersions ++ behaviorVersionsDataFor(team, dataService)
    )
  }

}
