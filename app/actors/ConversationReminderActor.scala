package actors

import java.time.OffsetDateTime
import javax.inject.Inject

import akka.actor.{Actor, ActorSystem}
import drivers.SlickPostgresDriver.api._
import models.behaviors.BotResultService
import models.behaviors.conversations.ConversationServices
import play.api.cache.CacheApi
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}
import services.{AWSLambdaService, DataService, SlackEventService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

object ConversationReminderActor {
  final val name = "conversationReminder"
}

class ConversationReminderActor @Inject()(
                                          val lambdaService: AWSLambdaService,
                                          val dataService: DataService,
                                          val slackEventService: SlackEventService,
                                          val cache: CacheApi,
                                          val ws: WSClient,
                                          val configuration: Configuration,
                                          val botResultService: BotResultService,
                                          implicit val actorSystem: ActorSystem
                                        ) extends Actor {

  // initial delay of 1 minute so that, in the case of errors & actor restarts, it doesn't hammer external APIs
  val tick = context.system.scheduler.schedule(1 minute, 1 minute, self, "tick")

  override def postStop() = {
    tick.cancel()
  }

  def remindAsNeeded(when: OffsetDateTime): Future[Unit] = {
    val action: DBIO[Boolean] = dataService.conversations.maybeNextNeedingReminderAction(when).flatMap { maybeNext =>
      maybeNext.map { convo =>
        val services = ConversationServices(dataService, lambdaService, slackEventService, cache, configuration, ws, actorSystem)
        convo.maybeRemindResultAction(services).flatMap { maybeResult =>
          maybeResult.map { result =>
            botResultService.sendInAction(result, None, None).flatMap { _ =>
              dataService.conversations.touchAction(convo).map(_ => true)
            }
          }.getOrElse {
            // Just act like the reminding happened if no remind result can be build (i.e. non-Slack message for now)
            dataService.conversations.touchAction(convo).map(_ => true)
          }
        }
      }.getOrElse(DBIO.successful(false))
    }
    val eventualShouldContinue: Future[Boolean] = dataService.run(action.transactionally).recover {
      case t: Throwable => {
        Logger.error(s"Exception reminding about a conversation", t)
        true
      }
    }
    eventualShouldContinue.flatMap { shouldContinue =>
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
