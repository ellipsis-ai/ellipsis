package models.behaviors.builtins

import com.amazonaws.AmazonServiceException
import models.behaviors.{BotResult, SimpleTextResult}
import models.behaviors.events.MessageContext
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class ResetBehaviorsBehavior(
                            messageContext: MessageContext,
                            lambdaService: AWSLambdaService,
                            dataService: DataService
                            ) extends BuiltinBehavior {

  def result: Future[BotResult] = {
    val eventualReply = try {
      for {
        maybeTeam <- dataService.teams.find(messageContext.teamId)
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
      SimpleTextResult(reply)
    }
  }

}
