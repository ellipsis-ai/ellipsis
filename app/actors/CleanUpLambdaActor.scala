package actors

import javax.inject.Inject

import akka.actor.Actor
import services.AWSLambdaService

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object CleanUpLambdaActor {
  final val name = "clean-up-lambda"
}

class CleanUpLambdaActor @Inject() (lambdaService: AWSLambdaService) extends Actor {

  // initial delay of 1 minute so that, in the case of errors & actor restarts, it doesn't hammer external APIs
  val tick = context.system.scheduler.schedule(1 minute, 1 hour, self, "tick")

  override def postStop() = {
    tick.cancel()
  }

  def cleanUp(functionName: String): Future[Unit] = {
    lambdaService.deleteFunction(functionName).map { _ =>
      println(functionName)
    }
  }

  def receive = {
    case "tick" => {
      for {
        partitioned <- lambdaService.partionedBehaviorGroupFunctionNames
        _ <- Future.sequence(partitioned.obsolete.map { ea =>
          cleanUp(ea)
        })
      } yield {}
    }
  }
}
