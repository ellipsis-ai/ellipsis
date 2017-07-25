package models.behaviors

import javax.inject.Inject

import akka.actor.ActorSystem
import services.DataService
import slick.dbio.DBIO

import scala.concurrent.Future

class BotResultServiceImpl @Inject() (
                                        dataService: DataService
                                      ) extends BotResultService {

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
      sendResult <- DBIO.from(event.sendMessage(botResult.fullText, forcePrivateResponse, maybeShouldUnfurl, maybeConversation, botResult.maybeActions))
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
