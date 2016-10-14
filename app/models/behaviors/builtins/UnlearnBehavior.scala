package models.behaviors.builtins

import com.amazonaws.AmazonServiceException
import models.behaviors.{BotResult, SimpleTextResult}
import models.behaviors.events.MessageContext
import services.{AWSLambdaService, DataService}
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class UnlearnBehavior(
                            patternString: String,
                            messageContext: MessageContext,
                            lambdaService: AWSLambdaService,
                            dataService: DataService
                            ) extends BuiltinBehavior {

  def result: Future[BotResult] = {
    val eventualReply = try {
      for {
        triggers <- dataService.messageTriggers.allWithExactPattern(patternString, messageContext.teamId)
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
      SimpleTextResult(reply, forcePrivateResponse = false)
    }
  }

}
