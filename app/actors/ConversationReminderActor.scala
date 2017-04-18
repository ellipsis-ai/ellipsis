package actors

import javax.inject.Inject

import akka.actor.{Actor, ActorSystem}
import play.api.cache.CacheApi
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

object ConversationReminderActor {
  final val name = "conversationReminder"
}

class ConversationReminderActor @Inject()(
                                          val lambdaService: AWSLambdaService,
                                          val dataService: DataService,
                                          val cache: CacheApi,
                                          val ws: WSClient,
                                          val configuration: Configuration,
                                          implicit val actorSystem: ActorSystem
                                        ) extends Actor {

  // initial delay of 1 minute so that, in the case of errors & actor restarts, it doesn't hammer external APIs
  val tick = context.system.scheduler.schedule(1 minute, 1 minute, self, "tick")

  override def postStop() = {
    tick.cancel()
  }

  def receive = {
    case "tick" => {
      dataService.conversations.allNeedingReminder.flatMap { pending =>
        Future.sequence(pending.map { ea =>
          ea.maybeRemindResult(lambdaService, dataService, cache, ws, configuration).flatMap { maybeResult =>
            maybeResult.map { result =>
              val intro = """───
                            |
                            |Hey, don’t forget, I’m still waiting for your answer to this:""".stripMargin
              result.sendIn(None, dataService, Some(intro)).flatMap { maybeSendResult =>
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
