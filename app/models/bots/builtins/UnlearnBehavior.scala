package models.bots.builtins

import com.amazonaws.AmazonServiceException
import models.bots.{BehaviorResult, SimpleTextResult}
import models.bots.events.MessageContext
import models.bots.triggers.MessageTriggerQueries
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

  def result: Future[BehaviorResult] = {
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
    val action = eventualReply.map { reply =>
      SimpleTextResult(reply)
    }
    dataService.run(action)
  }

}
