package models.behaviors

import javax.inject.Inject
import akka.actor.ActorSystem
import play.api.Configuration
import services.{DataService, DefaultServices}
import services.caching.CacheService
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

class BotResultServiceImpl @Inject() (
                                        services: DefaultServices,
                                        configuration: Configuration,
                                        implicit val ec: ExecutionContext
                                      ) extends BotResultService {

  val dataService: DataService = services.dataService
  val cacheService: CacheService = services.cacheService

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
