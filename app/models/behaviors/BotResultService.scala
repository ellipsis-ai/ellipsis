package models.behaviors

import akka.actor.ActorSystem
import slick.dbio.DBIO

import scala.concurrent.Future

trait BotResultService {

  def sendInAction(
                    botResult: BotResult,
                    maybeShouldUnfurl: Option[Boolean]
                  )(implicit actorSystem: ActorSystem): DBIO[Option[String]]

  def sendIn(
              botResult: BotResult,
              maybeShouldUnfurl: Option[Boolean]
            )(implicit actorSystem: ActorSystem): Future[Option[String]]

}
