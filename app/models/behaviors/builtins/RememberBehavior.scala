package models.behaviors.builtins

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import json.{BehaviorConfig, BehaviorGroupData, BehaviorTriggerData, BehaviorVersionData}
import models.behaviors._
import models.behaviors.events.Event
import services.{AWSLambdaService, DataService}
import utils.QuestionAnswerExtractor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RememberBehavior(event: Event, lambdaService: AWSLambdaService, dataService: DataService) extends BuiltinBehavior {

  def result(implicit actorSystem: ActorSystem): Future[BotResult] = {
    for {
      maybeTeam <- dataService.teams.find(event.teamId)
      maybeUser <- maybeTeam.map { team =>
        dataService.users.findFromEvent(event, team)
      }.getOrElse(Future.successful(None))
      messages <- event.recentMessages(dataService)
      qaExtractor <- Future.successful(QuestionAnswerExtractor(messages))
      maybeGroup <- maybeTeam.map { team =>
        dataService.behaviorGroups.createFor(None, None, None, None, team).map(Some(_))
      }.getOrElse(Future.successful(None))
      maybeVersionData <- Future.successful(maybeGroup.map { group =>
        val triggerData = qaExtractor.maybeLastQuestion.map { lastQuestion =>
          Seq(BehaviorTriggerData(lastQuestion, requiresMention = false, isRegex = false, caseSensitive = false))
        }.getOrElse(Seq())
        Some(
          BehaviorGroupData(
            Some(group.id),
            group.team.id,
            None,
            None,
            None,
            Seq(),
            Seq(),
            Seq(
              BehaviorVersionData.buildFor(
                group.team.id,
                None,
                None,
                isNewBehavior = false,
                None,
                "",
                qaExtractor.possibleAnswerContent,
                Seq(),
                triggerData,
                BehaviorConfig(None, None, None, None, None, None, None),
                None,
                None,
                None,
                dataService
              )
            ),
            None,
            None,
            OffsetDateTime.now
          )

        )
      }.getOrElse(None))
      maybeVersion <- (for {
        group <- maybeGroup
        user <- maybeUser
        data <- maybeVersionData
      } yield {
        dataService.behaviorGroupVersions.createFor(group, user, data).map(Some(_))
      }).getOrElse(Future.successful(None))
    } yield {
      maybeVersion.map { groupVersion =>
        val link = dataService.behaviorGroups.editLinkFor(groupVersion.group.id, lambdaService.configuration)
        SimpleTextResult(event, s"OK, I compiled recent messages into [a new skill]($link)", forcePrivateResponse = false)
      }.getOrElse{
        NoResponseResult(event, None)
      }
    }
  }

}
