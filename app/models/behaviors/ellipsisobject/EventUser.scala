package models.behaviors.ellipsisobject

import akka.actor.ActorSystem
import json.Formatting._
import json.UserData
import models.accounts.user.User
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import play.api.libs.json
import play.api.libs.json._
import services.DefaultServices
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

case class EventUser(user: User, identities: Seq[IdentityInfo], maybeUserData: Option[UserData]) {

  def toJson: JsObject = {
    val identitiesPart = Json.obj(
      "identities" -> json.JsArray(identities.map(_.toJson))
    )
    val userDataPart = maybeUserData.map(data => Json.toJsObject(data)).getOrElse(Json.obj())
    identitiesPart ++ userDataPart
  }
}

object EventUser {

  def buildForAction(
                      user: User,
                      event: Event,
                      maybeConversation: Option[Conversation],
                      services: DefaultServices
                    )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[EventUser] = {
    for {
      identities <- IdentityInfo.allForAction(user, services)
      maybeTeam <- services.dataService.teams.findAction(user.teamId)
      maybeUserData <- maybeTeam.map { team =>
        DBIO.from(services.dataService.users.userDataFor(user, team)).map(Some(_))
      }.getOrElse(DBIO.successful(None))
    } yield {
      EventUser(user, identities, maybeUserData)
    }
  }

  def buildForAction(
                      event: Event,
                      maybeConversation: Option[Conversation],
                      services: DefaultServices
                    )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[EventUser] = {
    for {
      user <- event.ensureUserAction(services.dataService)
      info <- buildForAction(user, event, maybeConversation, services)
    } yield info
  }

}
