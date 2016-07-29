package actors

import javax.inject.Inject

import akka.actor.Actor
import models.accounts.SlackBotProfile
import org.joda.time.DateTime
import org.postgresql.{PGConnection, PGNotification}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.SlackService
import slick.driver.PostgresDriver.api._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


object SlackBotProfileChangeListenerActor {
  final val name = "slackbot-profile-change-listener"
}

class SlackBotProfileChangeListenerActor @Inject() (val slackService: SlackService) extends Actor {

  val models = slackService.models

  implicit val slackBotProfileReads: Reads[SlackBotProfile] = (
    (JsPath \ "user_id").read[String] and
      (JsPath \ "team_id").read[String] and
      (JsPath \ "slack_team_id").read[String] and
      (JsPath \ "token").read[String] and
      (JsPath \ "created_at").read[DateTime]
    )(SlackBotProfile.apply _)

  val tick = context.system.scheduler.schedule(500 millis, 1000 millis, self, "tick")

  override def preStart() = {
    models.runNow(sqlu"LISTEN events")
  }

  override def postStop() = {
    tick.cancel()
  }

  def receive = {
    case "tick" => {
      val action = SimpleDBIO[Boolean] { context =>
        val pgConnection = context.connection.unwrap[PGConnection](classOf[PGConnection])
        val notifications = Option(pgConnection.getNotifications).getOrElse(Array[PGNotification]())

        notifications.foreach { ea =>
          val json = Json.parse(ea.getParameter)
          val action = (json \ "action").as[String]
          val profileJson = json \ "data"
          profileJson.validate[SlackBotProfile] match {
            case JsSuccess(profile, jsPath) => {
              action match {
                case "INSERT" | "UPDATE" => slackService.startFor(profile)
                case "DELETE" => slackService.stopFor(profile)
              }
            }
            case JsError(err) => println("oops")
          }
        }

        true
      }

      models.runNow(action)
    }
  }
}
