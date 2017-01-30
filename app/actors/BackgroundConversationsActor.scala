package actors

import javax.inject.Inject

import akka.actor.Actor
import models.behaviors.events.EventHandler
import play.api.cache.CacheApi
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}
import services.{AWSLambdaService, DataService}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object BackgroundConversationsActor {
  final val name = "background-conversations"
}

class BackgroundConversationsActor @Inject() (
                                              val dataService: DataService,
                                              val eventHandler: EventHandler,
                                              val configuration: Configuration,
                                              val lambdaService: AWSLambdaService,
                                              val ws: WSClient,
                                              val cache: CacheApi
                                            ) extends Actor {

  // initial delay of 1 minute so that, in the case of errors & actor restarts, it doesn't hammer external APIs
  val tick = context.system.scheduler.schedule(1 minute, 1 minute, self, "tick")

  override def postStop() = {
    tick.cancel()
  }

  def receive = {
    case "tick" => {
      dataService.conversations.allForeground.flatMap { convos =>
        Future.sequence(convos.map { convo =>
          if (convo.shouldBeBackgrounded) {
            convo.maybeEventForBackgrounding(dataService).flatMap { maybeEvent =>
              maybeEvent.map { event =>
                event.sendMessage(
                  "Looks like you weren't able to answer this right away. No problem! I've moved this conversation to:",
                  convo.behaviorVersion.forcePrivateResponse,
                  maybeShouldUnfurl = None,
                  Some(convo)
                ).flatMap { maybeLastTs =>
                  val convoWithThreadId = convo.copyWithMaybeThreadId(maybeLastTs)
                  dataService.conversations.save(convoWithThreadId).flatMap { _ =>
                    convoWithThreadId.respond(event, lambdaService, dataService, cache, ws, configuration).map { result =>
                      result.sendIn(None, Some(convoWithThreadId))
                    }
                  }
                }
              }.getOrElse(Future.successful({}))
            }
          } else {
            Future.successful({})
          }.recover {
            case t: Throwable => {
              Logger.error(s"Exception backgrounding conversation: ${convo.id}", t)
            }
          }
        })
      }.
        map { _ => true }.
        recover {
          case t: Throwable => {
            Logger.error("Exception backgrounding conversations", t)
          }
        }
    }
  }
}
