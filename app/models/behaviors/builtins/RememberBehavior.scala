package models.behaviors.builtins

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import json._
import models.behaviors._
import models.behaviors.events.Event
import play.api.libs.json.JsNull
import services.DefaultServices
import utils.QuestionAnswerExtractor

import scala.concurrent.{ExecutionContext, Future}

case class RememberBehavior(event: Event, services: DefaultServices) extends BuiltinBehavior {

  def result(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    val dataService = services.dataService
    val lambdaService = services.lambdaService
    for {
      maybeTeam <- dataService.teams.find(event.teamId)
      maybeUser <- maybeTeam.map { team =>
        dataService.users.findFromEvent(event, team)
      }.getOrElse(Future.successful(None))
      messages <- event.recentMessages(dataService)
      qaExtractor <- Future.successful(QuestionAnswerExtractor(messages))
      maybeGroup <- maybeTeam.map { team =>
        dataService.behaviorGroups.createFor(None, team).map(Some(_))
      }.getOrElse(Future.successful(None))
      maybeUserData <- (for {
        team <- maybeTeam
        user <- maybeUser
      } yield {
        dataService.users.userDataFor(user, team).map(Some(_))
      }).getOrElse(Future.successful(None))
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
                None,
                group.team.id,
                None,
                Some(group.id),
                isNew = false,
                None,
                "",
                qaExtractor.possibleAnswerContent,
                Seq(),
                triggerData,
                BehaviorConfig(None, None, None, isDataType = false, dataTypeConfig = None),
                None,
                None,
                None,
                dataService
              )
            ),
            Seq(),
            Seq(),
            Seq(),
            Seq(),
            None,
            None,
            Some(OffsetDateTime.now),
            maybeUserData
          )

        )
      }.getOrElse(None))
      maybeGroupVersion <- (for {
        group <- maybeGroup
        user <- maybeUser
        data <- maybeVersionData
      } yield {
        dataService.behaviorGroupVersions.createFor(group, user, data, forceNodeModuleUpdate = false).map(Some(_))
      }).getOrElse(Future.successful(None))
      maybeBehaviorVersion <- maybeGroupVersion.map { groupVersion =>
        dataService.behaviorVersions.allForGroupVersion(groupVersion).map(_.headOption)
      }.getOrElse(Future.successful(None))
    } yield {
      maybeGroupVersion.map { groupVersion =>
        val link = maybeBehaviorVersion.map { behaviorVersion =>
          dataService.behaviors.editLinkFor(groupVersion.group.id, Some(behaviorVersion.behavior.id), lambdaService.configuration)
        }.getOrElse {
          dataService.behaviorGroups.editLinkFor(groupVersion.group.id, lambdaService.configuration)
        }
        SimpleTextResult(event, None, s"OK, I compiled recent messages into [a new skill]($link)", forcePrivateResponse = false)
      }.getOrElse{
        NoResponseResult(event, None, JsNull, None)
      }
    }
  }

}
