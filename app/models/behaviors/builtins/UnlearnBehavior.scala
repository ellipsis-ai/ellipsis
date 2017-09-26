package models.behaviors.builtins

import akka.actor.ActorSystem
import com.amazonaws.AmazonServiceException
import models.behaviors.events.Event
import models.behaviors.{BotResult, SimpleTextResult}
import services.DefaultServices

import scala.concurrent.{ExecutionContext, Future}

case class UnlearnBehavior(
                            patternString: String,
                            event: Event,
                            services: DefaultServices
                          ) extends BuiltinBehavior {

  def result(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    val dataService = services.dataService
    val eventualReply = try {
      for {
        triggers <- dataService.messageTriggers.allWithExactPattern(patternString, event.teamId)
        _ <- Future.sequence(triggers.map { trigger =>
          dataService.behaviorVersions.unlearn(trigger.behaviorVersion)
        })
      } yield {
        s"$patternString? Never heard of it."
      }
    } catch {
      case e: AmazonServiceException => Future.successful("D'oh! That didn't work.")
    }
    eventualReply.map { reply =>
      SimpleTextResult(event, None, reply, forcePrivateResponse = false)
    }
  }

}
