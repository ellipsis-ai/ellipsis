package models.bots.builtins

import com.amazonaws.AmazonServiceException
import models.bots.events.MessageContext
import models.bots.triggers.MessageTriggerQueries
import services.AWSLambdaService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global

case class UnlearnBehavior(
                            patternString: String,
                            messageContext: MessageContext,
                            lambdaService: AWSLambdaService
                            ) extends BuiltinBehavior {

  def run: DBIO[Unit] = {
    val eventualReply = try {
      for {
        triggers <- MessageTriggerQueries.allWithExactPattern(patternString, messageContext.teamId)
        _ <- DBIO.sequence(triggers.map(_.behaviorVersion.unlearn(lambdaService)))
      } yield {
        s"$patternString? Never heard of it."
      }
    } catch {
      case e: AmazonServiceException => DBIO.successful("D'oh! That didn't work.")
    }
    eventualReply.map { reply =>
      messageContext.sendMessage(reply)
    }
  }

}
