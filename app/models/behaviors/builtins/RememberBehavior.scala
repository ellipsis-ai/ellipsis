package models.behaviors.builtins

import json.{BehaviorConfig, BehaviorTriggerData, BehaviorVersionData}
import models.behaviors._
import services.slack.MessageEvent
import services.{AWSLambdaService, DataService}
import utils.QuestionAnswerExtractor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RememberBehavior(event: MessageEvent, lambdaService: AWSLambdaService, dataService: DataService) extends BuiltinBehavior {

  def result: Future[BotResult] = {
    for {
      maybeTeam <- dataService.teams.find(event.teamId)
      maybeUser <- maybeTeam.map { team =>
        dataService.users.findFromMessageEvent(event, team)
      }.getOrElse(Future.successful(None))
      messages <- event.recentMessages(dataService)
      qaExtractor <- Future.successful(QuestionAnswerExtractor(messages))
      maybeBehavior <- maybeTeam.map { team =>
        dataService.behaviors.createFor(team, None, None).map(Some(_))
      }.getOrElse(Future.successful(None))
      maybeVersionData <- Future.successful(maybeBehavior.map { behavior =>
        val triggerData = qaExtractor.maybeLastQuestion.map { lastQuestion =>
          Seq(BehaviorTriggerData(lastQuestion, requiresMention = false, isRegex = false, caseSensitive = false))
        }.getOrElse(Seq())
        Some(
          BehaviorVersionData.buildFor(
            behavior.team.id,
            behavior.maybeGroup.map(_.id),
            behavior.maybeGroup.map(_.name),
            behavior.maybeGroup.flatMap(_.maybeDescription),
            Some(behavior.id),
            None,
            "",
            qaExtractor.possibleAnswerContent,
            Seq(),
            triggerData,
            BehaviorConfig(None, None, None, None, None, None),
            None,
            None,
            None,
            dataService
          )
        )
      }.getOrElse(None))
      maybeBehaviorVersion <- (for {
        behavior <- maybeBehavior
        data <- maybeVersionData
      } yield {
        dataService.behaviorVersions.createFor(behavior, maybeUser, data).map(Some(_))
      }).getOrElse(Future.successful(None))
    } yield {
      maybeBehaviorVersion.map { behaviorVersion =>
        val link = behaviorVersion.editLinkFor(lambdaService.configuration)
        SimpleTextResult(s"OK, I compiled recent messages into [a new skill]($link)", forcePrivateResponse = false)
      }.getOrElse{
        NoResponseResult(None)
      }
    }
  }

}
