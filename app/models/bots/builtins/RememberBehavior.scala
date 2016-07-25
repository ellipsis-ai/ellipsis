package models.bots.builtins

import models.Team
import models.accounts.User
import models.bots.triggers.MessageTriggerQueries
import models.bots.{BehaviorVersionQueries, BehaviorQueries, MessageContext}
import services.AWSLambdaService
import utils.QuestionAnswerExtractor
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

case class RememberBehavior(messageContext: MessageContext, lambdaService: AWSLambdaService) extends BuiltinBehavior {

  def run: DBIO[Unit] = {
    for {
      maybeTeam <- Team.find(messageContext.teamId)
      maybeUser <- maybeTeam.map { team =>
        User.findFromMessageContext(messageContext, team)
      }.getOrElse(DBIO.successful(None))
      messages <- messageContext.recentMessages
      qaExtractor <- DBIO.successful(QuestionAnswerExtractor(messages))
      maybeBehavior <- maybeTeam.map { team =>
        BehaviorQueries.createFor(team, None).map(Some(_))
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
      maybeBehaviorVersion.foreach { behaviorVersion =>
        behaviorVersion.editLinkFor(lambdaService.configuration).foreach { link =>
          messageContext.sendMessage(s"OK, I compiled recent messages at $link")
        }
      }

    }
  }

}
