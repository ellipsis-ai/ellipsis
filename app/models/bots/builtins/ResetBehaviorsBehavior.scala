package models.bots.builtins

import com.amazonaws.AmazonServiceException
import models.bots.{BehaviorQueries, BehaviorResult, SimpleTextResult}
import models.bots.events.MessageContext
import services.{AWSLambdaService, DataService}
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global

case class ResetBehaviorsBehavior(
                            messageContext: MessageContext,
                            lambdaService: AWSLambdaService,
                            dataService: DataService
                            ) extends BuiltinBehavior {

  def result: DBIO[BehaviorResult] = {
    val eventualReply = try {
      for {
        maybeTeam <- DBIO.from(dataService.teams.find(messageContext.teamId))
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
      SimpleTextResult(reply)
    }
  }

}
