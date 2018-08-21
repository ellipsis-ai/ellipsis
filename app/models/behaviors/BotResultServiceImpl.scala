package models.behaviors

import javax.inject.Inject
import akka.actor.ActorSystem
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.{Event, EventHandler, RunEvent}
import play.api.{Configuration, Logger}
import services.caching.CacheService
import services.slack.SlackEventService
import services.{DataService, DefaultServices}
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

  private def runNextAction(nextAction: NextAction, botResult: BotResult)(implicit actorSystem: ActorSystem): DBIO[Unit] = {
    for {
      maybeSlackChannelId <- botResult.maybeBehaviorVersion.map { behaviorVersion =>
        DBIO.from(botResult.event.maybeChannelToUseFor(behaviorVersion, services))
      }.getOrElse(DBIO.successful(None))
      maybeBehaviorVersion <- botResult.maybeBehaviorVersion.map { originatingBehaviorVersion =>
        dataService.behaviorVersions.findByNameAction(nextAction.actionName, originatingBehaviorVersion.groupVersion)
      }.getOrElse(DBIO.successful(None))
      maybeBotProfile <- maybeBehaviorVersion.map { behaviorVersion =>
        dataService.slackBotProfiles.allForAction(behaviorVersion.team).map(_.headOption)
      }.getOrElse(DBIO.successful(None))
      user <- botResult.event.ensureUserAction(dataService)
      maybeSlackLinkedAccount <- dataService.linkedAccounts.maybeForSlackForAction(user)
      maybeSlackTeamIdForUser <- DBIO.from(dataService.users.maybeSlackTeamIdFor(user))
      maybeEvent <- DBIO.successful(
        for {
          botProfile <- maybeBotProfile
          slackTeamIdForUser <- maybeSlackTeamIdForUser
          linkedAccount <- maybeSlackLinkedAccount
          behaviorVersion <- maybeBehaviorVersion
          channel <- maybeSlackChannelId
        } yield RunEvent(
          botProfile,
          slackTeamIdForUser,
          behaviorVersion,
          nextAction.argumentsMap,
          channel,
          None,
          linkedAccount.loginInfo.providerKey,
          SlackTimestamp.now,
          Some(botResult.event.eventType)
        )
      )
      _ <- if (maybeBehaviorVersion.isDefined) {
        runBehaviorFor(maybeEvent, botResult.maybeBehaviorVersion)
      } else {
        val text = s"Can't run action named `${nextAction.actionName}` in this skill"
        val result = SimpleTextResult(botResult.event, botResult.maybeConversation, text, botResult.forcePrivateResponse)
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
    val forcePrivateResponse = botResult.forcePrivateResponse
    for {
      didInterrupt <- if (botResult.shouldInterrupt) {
        botResult.interruptOngoingConversationsForAction(services)
      } else {
        DBIO.successful(false)
      }
      files <- try {
        DBIO.successful(botResult.files)
      } catch {
        case e: InvalidFilesException => {
          sendInAction(SimpleTextResult(event, maybeConversation, e.responseText, forcePrivateResponse), None).map(_ => Seq())
        }
      }
      maybeChoices <- botResult.maybeChoicesAction(dataService)
      sendResult <- DBIO.from(
        event.sendMessage(
          botResult.fullText,
          forcePrivateResponse,
          maybeShouldUnfurl,
          maybeConversation,
          botResult.attachmentGroups,
          files,
          maybeChoices.getOrElse(Seq()),
          botResult.developerContext,
          services,
          configuration
        )
      )
      _ <- botResult.maybeNextAction.map { nextAction =>
        runNextAction(nextAction, botResult)
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
