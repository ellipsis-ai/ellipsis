package actors

import java.time.OffsetDateTime
import javax.inject.Inject

import akka.actor.Actor
import drivers.SlickPostgresDriver.api._
import play.api.Logger
import services.DefaultServices

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

object ConversationReminderActor {
  final val name = "conversationReminder"
}

class ConversationReminderActor @Inject()(val services: DefaultServices) extends Actor {

  val dataService = services.dataService
  implicit val actorSystem = services.actorSystem

  // initial delay of 1 minute so that, in the case of errors & actor restarts, it doesn't hammer external APIs
  val tick = context.system.scheduler.schedule(1 minute, 1 minute, self, "tick")

  override def postStop() = {
    tick.cancel()
  }

  def remindAsNeeded(when: OffsetDateTime): Future[Unit] = {
    val action: DBIO[Boolean] = dataService.conversations.maybeNextNeedingReminderAction(when).flatMap { maybeNext =>
      maybeNext.map { convo =>
        dataService.conversations.touchAction(convo).flatMap { _ =>
          convo.maybeRemindResultAction(services).flatMap { maybeResult =>
            maybeResult.map { result =>
              services.botResultService.sendInAction(result, None, None).map(_ => true)
            }.getOrElse(DBIO.successful(true))
          }
        }
      }.getOrElse(DBIO.successful(false))
    }
    dataService.run(action.transactionally).flatMap { shouldContinue =>
      if (shouldContinue) {
        remindAsNeeded(when)
      } else {
        Future.successful({})
      }
    }.recover {
      case t: Throwable => {
        Logger.error("Exception reminding about conversations", t)
      }
    }
  }

  def receive = {
    case "tick" => remindAsNeeded(OffsetDateTime.now)
  }
}
