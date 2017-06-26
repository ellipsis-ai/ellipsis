package actors

import javax.inject.Inject

import akka.actor.Actor
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

  def receive = {
    case "tick" => {
      dataService.conversations.allNeedingReminder.flatMap { pending =>
        Future.sequence(pending.map { ea =>
          ea.maybeRemindResult(services).flatMap { maybeResult =>
            maybeResult.map { result =>
              result.sendIn(None, dataService, None).flatMap { maybeSendResult =>
                dataService.conversations.touch(ea)
              }
            }.getOrElse(Future.successful(None))
          }.recover {
            case t: Throwable => {
              Logger.error(s"Exception reminding about conversation with ID: ${ea.id}", t)
            }
          }
        })
      }.
        map { _ => true }.
        recover {
          case t: Throwable => {
            Logger.error("Exception reminding about conversations", t)
          }
        }
    }
  }
}
