package models.bots.builtins

import com.amazonaws.AmazonServiceException
import models.Team
import models.bots.BehaviorQueries
import models.bots.events.MessageContext
import services.AWSLambdaService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global

case class ResetBehaviorsBehavior(
                            messageContext: MessageContext,
                            lambdaService: AWSLambdaService
                            ) extends BuiltinBehavior {

  def run: DBIO[Unit] = {
    val eventualReply = try {
      for {
        maybeTeam <- Team.find(messageContext.teamId)
        behaviors <- maybeTeam.map { team =>
          BehaviorQueries.allForTeam(team)
        }.getOrElse(DBIO.successful(Seq()))
        _ <- DBIO.sequence(behaviors.map(_.unlearn(lambdaService)))
      } yield {
        "OK, I've forgotten all the things"
      }
    } catch {
      case e: AmazonServiceException => DBIO.successful("Got an error from AWS")
    }
    eventualReply.map { reply =>
      messageContext.sendMessage(reply)
    }
  }

}
