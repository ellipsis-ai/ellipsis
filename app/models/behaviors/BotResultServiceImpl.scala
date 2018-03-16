package models.behaviors

import javax.inject.Inject

import akka.actor.ActorSystem
import models.behaviors.events.{Event, EventHandler, RunEvent}
import play.api.{Configuration, Logger}
import services.caching.CacheService
import services.{DataService, DefaultServices, SlackEventService}
import slick.dbio.DBIO
import utils.SlackTimestamp

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

  private def runBehaviorFor(maybeEvent: Option[Event])(implicit actorSystem: ActorSystem): DBIO[Seq[BotResult]] = {
    for {
      result <- maybeEvent.map { event =>
        DBIO.from(eventHandler.handle(event, None)).flatMap { results =>
          DBIO.sequence(results.map { result =>
            sendInAction(result, None, None, None).map { _ =>
              Logger.info(event.logTextFor(result, Some("as next action")))
            }.map(_ => result)
          })
        }
      }.getOrElse {
        DBIO.successful(Seq())
      }
    } yield result
  }

  private def run(nextAction: NextAction, botResult: BotResult)(implicit actorSystem: ActorSystem): DBIO[Unit] = {
    for {
      maybeSlackChannelId <- botResult.event.maybeChannelForSendAction(botResult.forcePrivateResponse, botResult.maybeConversation, dataService)
      maybeBehavior <- botResult.maybeBehaviorVersion.map { originatingBehaviorVersion =>
        dataService.behaviors.findByNameAction(nextAction.actionName, originatingBehaviorVersion.group)
      }.getOrElse(DBIO.successful(None))
      maybeBotProfile <- maybeBehavior.map { behavior =>
        dataService.slackBotProfiles.allForAction(behavior.team).map(_.headOption)
      }.getOrElse(DBIO.successful(None))
      user <- botResult.event.ensureUserAction(dataService)
      maybeSlackLinkedAccount <- dataService.linkedAccounts.maybeForSlackForAction(user)
      maybeSlackProfile <- maybeSlackLinkedAccount.map { slackLinkedAccount =>
        dataService.slackProfiles.findAction(slackLinkedAccount.loginInfo)
      }.getOrElse(DBIO.successful(None))
      maybeEvent <- DBIO.successful(
        for {
          botProfile <- maybeBotProfile
          slackProfile <- maybeSlackProfile
          behavior <- maybeBehavior
          channel <- maybeSlackChannelId
        } yield RunEvent(
          botProfile,
          behavior,
          nextAction.argumentsMap,
          channel,
          None,
          slackProfile.loginInfo.providerKey,
          SlackTimestamp.now,
          slackService.clientFor(botProfile),
          Some(botResult.event.eventType)
        )
      )
      _ <- if (maybeBehavior.isDefined) {
        runBehaviorFor(maybeEvent)
      } else {
        val text = s"Can't run action named `${nextAction.actionName}` in this skill"
        val result = SimpleTextResult(botResult.event, botResult.maybeConversation, text, botResult.forcePrivateResponse)
        sendInAction(result, None, None, None)
      }
    } yield {}
  }

  def sendInAction(
                    botResult: BotResult,
                    maybeShouldUnfurl: Option[Boolean],
                    maybeIntro: Option[String] = None,
                    maybeInterruptionIntro: Option[String] = None
                  )(implicit actorSystem: ActorSystem): DBIO[Option[String]] = {
    if (!botResult.shouldSend) {
      return DBIO.successful(None)
    }
    botResult.beforeSend
    val event = botResult.event
    val maybeConversation = botResult.maybeConversation
    val forcePrivateResponse = botResult.forcePrivateResponse
    for {
      didInterrupt <- if (botResult.shouldInterrupt) {
        botResult.interruptOngoingConversationsForAction(dataService)
      } else {
        DBIO.successful(false)
      }
      _ <- maybeIntro.map { intro =>
        val introToSend = if (didInterrupt) {
          maybeInterruptionIntro.getOrElse(intro)
        } else {
          intro
        }
        val result = SimpleTextResult(event, maybeConversation, introToSend, forcePrivateResponse)
        sendInAction(result, None)
      }.getOrElse {
        DBIO.successful({})
      }
      files <- try {
        DBIO.successful(botResult.files)
      } catch {
        case e: InvalidFilesException => {
          sendInAction(SimpleTextResult(event, maybeConversation, e.responseText, forcePrivateResponse), None).map(_ => Seq())
        }
      }
      sendResult <- DBIO.from(
        event.sendMessage(
          botResult.fullText,
          forcePrivateResponse,
          maybeShouldUnfurl,
          maybeConversation,
          botResult.attachmentGroups,
          files,
          botResult.isForUndeployed,
          botResult.hasUndeployedVersionForAuthor,
          services,
          configuration
        )
      )
      _ <- botResult.maybeNextAction.map { nextAction =>
        run(nextAction, botResult)
      }.getOrElse(DBIO.successful({}))
    } yield sendResult
  }

  def sendIn(
              botResult: BotResult,
              maybeShouldUnfurl: Option[Boolean],
              maybeIntro: Option[String] = None,
              maybeInterruptionIntro: Option[String] = None
            )(implicit actorSystem: ActorSystem): Future[Option[String]] = {
    dataService.run(sendInAction(botResult, maybeShouldUnfurl, maybeIntro, maybeInterruptionIntro))
  }

}
