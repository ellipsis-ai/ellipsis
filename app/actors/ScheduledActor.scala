package actors

import java.time.OffsetDateTime
import javax.inject.Inject

import akka.actor.{Actor, ActorSystem}
import drivers.SlickPostgresDriver.api._
import models.behaviors.BotResultService
import models.behaviors.events.EventHandler
import models.behaviors.scheduling.Scheduled
import play.api.{Configuration, Logger}
import services.{DataService, DefaultServices}
import slack.api.SlackApiClient

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

object ScheduledActor {
  final val name = "scheduled"
}

class ScheduledActor @Inject()(
                                val services: DefaultServices,
                                val eventHandler: EventHandler,
                                implicit val actorSystem: ActorSystem,
                                implicit val ec: ExecutionContext
                              ) extends Actor {

  val dataService: DataService = services.dataService

  // initial delay of 1 minute so that, in the case of errors & actor restarts, it doesn't hammer external APIs
  val tick = context.system.scheduler.schedule(1 minute, 1 minute, self, "tick")

  override def postStop() = {
    tick.cancel()
  }

  def sendAsNeeded(when: OffsetDateTime): Future[Unit] = {
    val action: DBIO[Boolean] = Scheduled.maybeNextToBeSentAction(when, dataService).flatMap { maybeNext =>
      maybeNext.map { scheduled =>
        for {
          displayText <- DBIO.from(scheduled.displayText(dataService))
          maybeProfile <- scheduled.botProfileAction(dataService)
          maybeSlackUserId <- scheduled.maybeUser.map { user =>
            DBIO.from(dataService.linkedAccounts.maybeSlackUserIdFor(user))
          }.getOrElse(DBIO.successful(None))
          _ <- maybeProfile.map { profile =>
            scheduled.updateNextTriggeredForAction(dataService).flatMap { _ =>
              DBIO.from(scheduled.send(eventHandler, new SlackApiClient(profile.token), profile, services).recover {
                case t: Throwable => {
                  val user = scheduled.maybeUser.map { user =>
                    s"Ellipsis ID ${user.id} / Slack ID ${maybeSlackUserId.getOrElse("(unknown)")}"
                  }.getOrElse("(none)")
                  val message =
                    s"""Exception handling scheduled message:
                       |Team: ${scheduled.team.name} (${scheduled.team.id})
                       |User: $user
                       |Channel: ${scheduled.maybeChannel.getOrElse("(missing)")}
                       |Send privately: ${scheduled.isForIndividualMembers.toString}
                       |Summary: $displayText
                       |""".stripMargin
                  Logger.error(message, t)
                }
              })
            }
          }.getOrElse(DBIO.successful({}))
        } yield true
      }.getOrElse(DBIO.successful(false))
    }
    dataService.run(action.transactionally).flatMap { didSend =>
      if (didSend) {
        sendAsNeeded(when)
      } else {
        Future.successful({})
      }
    }
  }

  def receive = {
    case "tick" => sendAsNeeded(OffsetDateTime.now)
  }
}
