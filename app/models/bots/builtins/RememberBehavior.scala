package models.bots.builtins

import models.bots.events.MessageContext
import models.bots.triggers.MessageTriggerQueries
import models.bots._
import services.{AWSLambdaService, DataService}
import utils.QuestionAnswerExtractor
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RememberBehavior(messageContext: MessageContext, lambdaService: AWSLambdaService, dataService: DataService) extends BuiltinBehavior {

  def result: Future[BehaviorResult] = {
    val action = for {
      maybeTeam <- DBIO.from(dataService.teams.find(messageContext.teamId))
      maybeUser <- maybeTeam.map { team =>
        DBIO.from(dataService.users.findFromMessageContext(messageContext, team))
      }.getOrElse(DBIO.successful(None))
      messages <- DBIO.from(messageContext.recentMessages(dataService))
      qaExtractor <- DBIO.successful(QuestionAnswerExtractor(messages))
      maybeBehavior <- maybeTeam.map { team =>
        DBIO.from(dataService.behaviors.createFor(team, None)).map(Some(_))
      }.getOrElse(DBIO.successful(None))
      maybeBehaviorVersion <- maybeBehavior.map { behavior =>
        BehaviorVersionQueries.createFor(behavior, maybeUser).flatMap { behaviorVersion =>
          behaviorVersion.copy(maybeResponseTemplate = Some(qaExtractor.possibleAnswerContent)).save.flatMap { behaviorWithContent =>
            qaExtractor.maybeLastQuestion.map { lastQuestion =>
              MessageTriggerQueries.createFor(behaviorVersion, lastQuestion, requiresBotMention = false, shouldTreatAsRegex = false, isCaseSensitive = false)
            }.getOrElse {
              DBIO.successful(Unit)
            }.map(_ => behaviorWithContent)
          }
        }.map(Some(_)) transactionally
      }.getOrElse(DBIO.successful(None))
    } yield {
      maybeBehaviorVersion.map { behaviorVersion =>
        val link = behaviorVersion.editLinkFor(lambdaService.configuration)
        SimpleTextResult(s"OK, I compiled recent messages into [a new behavior]($link)")
      }.getOrElse{
        NoResponseResult(None)
      }
    }
    dataService.run(action)
  }

}
