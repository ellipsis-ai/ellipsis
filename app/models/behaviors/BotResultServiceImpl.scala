package models.behaviors

import akka.actor.ActorSystem
import javax.inject.Inject
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.{Event, EventHandler}
import play.api.{Configuration, Logger}
import services.caching.CacheService
import services.slack.SlackEventService
import services.{DataService, DefaultServices}
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

class BotResultServiceImpl @Inject() (
                                        services: DefaultServices,
                                        configuration: Configuration,
                                        eventHandler: EventHandler,
                                        implicit val ec: ExecutionContext
                                      ) extends BotResultService {

  val dataService: DataService = services.dataService
  val cacheService: CacheService = services.cacheService
  def slackService: SlackEventService = services.slackEventService

  private def runBehaviorFor(maybeEvent: Option[Event], maybeOriginatingBehaviorVersion: Option[BehaviorVersion])(implicit actorSystem: ActorSystem): DBIO[Seq[BotResult]] = {
    for {
      result <- maybeEvent.map { event =>
        DBIO.from(eventHandler.handle(event, None)).flatMap { results =>
          DBIO.sequence(results.map { result =>
            sendInAction(result, None).map { _ =>
              val nextActionPart = result.maybeBehaviorVersion.map(_.nameAndIdString).getOrElse("<not found>")
              val originatingActionPart = maybeOriginatingBehaviorVersion.map(_.nameAndIdString).getOrElse("<not found>")
              Logger.info(event.logTextFor(result, Some(s"as next action `${nextActionPart}` from action `${originatingActionPart}`")))
            }.map(_ => result)
          })
        }
      }.getOrElse {
        DBIO.successful(Seq())
      }
    } yield result
  }

  private def runNextAction(
                             nextAction: NextAction,
                             botResult: BotResult,
                             maybeMessageTs: Option[String]
                           )(implicit actorSystem: ActorSystem): DBIO[Unit] = {
    for {
      maybeOriginatingResponseChannel <- botResult.maybeBehaviorVersion.map { behaviorVersion =>
        DBIO.from(botResult.event.maybeChannelToUseFor(behaviorVersion, services))
      }.getOrElse(DBIO.successful(None))
      maybeBehaviorVersion <- botResult.maybeBehaviorVersion.map { originatingBehaviorVersion =>
        dataService.behaviorVersions.findByNameAction(nextAction.actionName, originatingBehaviorVersion.groupVersion)
      }.getOrElse(DBIO.successful(None))
      maybeEvent <- DBIO.successful(
        for {
          behaviorVersion <- maybeBehaviorVersion
          channel <- maybeOriginatingResponseChannel
        } yield {
          botResult.event.eventContext.newRunEventFor(botResult, nextAction, behaviorVersion, channel, maybeMessageTs)
        }
      )
      _ <- if (maybeBehaviorVersion.isDefined) {
        runBehaviorFor(maybeEvent, botResult.maybeBehaviorVersion)
      } else {
        val text = s"Can't run action named `${nextAction.actionName}` in this skill"
        val result = SimpleTextResult(botResult.event, botResult.maybeConversation, text, botResult.responseType)
        sendInAction(result, None)
      }
    } yield {}
  }

  def sendInAction(
                    botResult: BotResult,
                    maybeShouldUnfurl: Option[Boolean]
                  )(implicit actorSystem: ActorSystem): DBIO[Option[String]] = {
    if (!botResult.shouldSend) {
      return DBIO.successful(None)
    }
    botResult.beforeSend
    val event = botResult.event
    val maybeConversation = botResult.maybeConversation
    for {
      didInterrupt <- if (botResult.shouldInterrupt) {
        botResult.interruptOngoingConversationsForAction(services)
      } else {
        DBIO.successful(false)
      }
      user <- event.ensureUserAction(dataService)
      sendResult <- DBIO.from(
        event.sendMessage(
          botResult.fullText,
          botResult.responseType,
          maybeShouldUnfurl,
          maybeConversation,
          botResult.attachments,
          botResult.files,
          botResult.actionChoicesFor(user),
          botResult.developerContext,
          services
        )
      )
      _ <- botResult.maybeNextAction.map { nextAction =>
        runNextAction(nextAction, botResult, sendResult)
      }.getOrElse(DBIO.successful({}))
    } yield sendResult
  }

  def sendIn(
              botResult: BotResult,
              maybeShouldUnfurl: Option[Boolean]
            )(implicit actorSystem: ActorSystem): Future[Option[String]] = {
    dataService.run(sendInAction(botResult, maybeShouldUnfurl))
  }

}
