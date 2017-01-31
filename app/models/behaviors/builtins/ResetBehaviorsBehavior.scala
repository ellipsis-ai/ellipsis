package models.behaviors.builtins

import akka.actor.ActorSystem
import com.amazonaws.AmazonServiceException
import models.behaviors.events.MessageEvent
import models.behaviors.{BotResult, SimpleTextResult}
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class ResetBehaviorsBehavior(
                                   event: MessageEvent,
                                   lambdaService: AWSLambdaService,
                                   dataService: DataService
                            ) extends BuiltinBehavior {

  def result(implicit actorSystem: ActorSystem): Future[BotResult] = {
    val eventualReply = try {
      for {
        maybeTeam <- dataService.teams.find(event.teamId)
        groups <- maybeTeam.map { team =>
          dataService.behaviorGroups.allFor(team)
        }.getOrElse(Future.successful(Seq()))
        _ <- Future.sequence(groups.map(dataService.behaviorGroups.delete))
      } yield {
        "OK, I've forgotten all the things"
      }
    } catch {
      case e: AmazonServiceException => Future.successful("Got an error from AWS")
    }
    eventualReply.map { reply =>
      SimpleTextResult(event, reply, forcePrivateResponse = false)
    }
  }

}
