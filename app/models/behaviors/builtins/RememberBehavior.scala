package models.behaviors.builtins

import json.{BehaviorConfig, BehaviorTriggerData, BehaviorVersionData}
import models.behaviors.events.MessageContext
import models.behaviors._
import services.{AWSLambdaService, DataService}
import utils.QuestionAnswerExtractor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RememberBehavior(messageContext: MessageContext, lambdaService: AWSLambdaService, dataService: DataService) extends BuiltinBehavior {

  def result: Future[BotResult] = {
    for {
      maybeTeam <- dataService.teams.find(messageContext.teamId)
      maybeUser <- maybeTeam.map { team =>
        dataService.users.findFromMessageContext(messageContext, team)
      }.getOrElse(Future.successful(None))
      messages <- messageContext.recentMessages(dataService)
      qaExtractor <- Future.successful(QuestionAnswerExtractor(messages))
      maybeBehavior <- maybeTeam.map { team =>
        dataService.behaviors.createFor(team, None).map(Some(_))
      }.getOrElse(Future.successful(None))
      maybeVersionData <- Future.successful(maybeBehavior.map { behavior =>
        val triggerData = qaExtractor.maybeLastQuestion.map { lastQuestion =>
          Seq(BehaviorTriggerData(lastQuestion, requiresMention = false, isRegex = false, caseSensitive = false))
        }.getOrElse(Seq())
        Some(
          BehaviorVersionData.buildFor(
            behavior.team.id,
            Some(behavior.id),
            "",
            qaExtractor.possibleAnswerContent,
            Seq(),
            triggerData,
            BehaviorConfig(None, None, None, None),
            None,
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
        SimpleTextResult(s"OK, I compiled recent messages into [a new behavior]($link)")
      }.getOrElse{
        NoResponseResult(None)
      }
    }
  }

}
