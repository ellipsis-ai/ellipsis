package models.behaviors.builtins

import com.amazonaws.AmazonServiceException
import models.behaviors.{BotResult, SimpleTextResult}
import services.slack.NewMessageEvent
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class ResetBehaviorsBehavior(
                                   event: NewMessageEvent,
                                   lambdaService: AWSLambdaService,
                                   dataService: DataService
                            ) extends BuiltinBehavior {

  def result: Future[BotResult] = {
    val eventualReply = try {
      for {
        maybeTeam <- dataService.teams.find(event.teamId)
        behaviors <- maybeTeam.map { team =>
          dataService.behaviors.allForTeam(team)
        }.getOrElse(Future.successful(Seq()))
        _ <- Future.sequence(behaviors.map(b => dataService.behaviors.unlearn(b)))
      } yield {
        "OK, I've forgotten all the things"
      }
    } catch {
      case e: AmazonServiceException => Future.successful("Got an error from AWS")
    }
    eventualReply.map { reply =>
      SimpleTextResult(reply, forcePrivateResponse = false)
    }
  }

}
