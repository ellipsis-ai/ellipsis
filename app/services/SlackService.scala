package services

import javax.inject._
import models._
import models.accounts.{SlackBotProfileQueries, SlackBotProfile}
import models.bots.{SlackContext, SlackMessageEvent, RegexMessageTriggerQueries}
import play.api.inject.ApplicationLifecycle
import slack.rtm.SlackRtmClient
import akka.actor.ActorSystem
import slick.driver.PostgresDriver.api._
import scala.concurrent.Future
import scala.concurrent.duration._

@Singleton
class SlackService @Inject() (lambdaService: AWSLambdaService, appLifecycle: ApplicationLifecycle, models: Models) {

  implicit val system = ActorSystem("slack")
  implicit val ec = system.dispatcher

  start

  appLifecycle.addStopHook { () =>
    val action = allProfiles.map { profiles =>
      profiles.foreach { profile =>
        closeFor(profile)
      }
    }
    models.run(action)
  }

  def startFor(profile: SlackBotProfile) {

    val client = SlackRtmClient(profile.token, 5.seconds)
    val selfId = client.state.self.id

    client.onMessage { message =>
      if (message.user != selfId) {
        val action = for {
          maybeTeam <- Team.find(profile.teamId)
          behaviorResponses <- maybeTeam.map { team =>
            val messageContext = SlackContext(client, profile, message)
            RegexMessageTriggerQueries.behaviorResponsesFor(SlackMessageEvent(messageContext), team)
          }.getOrElse(DBIO.successful(Seq()))
        } yield {
            behaviorResponses.foreach(_.run(lambdaService))
          }
        models.runNow(action)
      }
    }

  }

  private def allProfiles: DBIO[Seq[SlackBotProfile]] = {
    SlackBotProfileQueries.all.result
  }

  def start: Future[Any] = {
    val action = allProfiles.map { profiles =>
      profiles.foreach { profile =>
        startFor(profile)
      }
    }
    models.run(action)
  }

  def closeFor(profile: SlackBotProfile): Unit = {
    val client = SlackRtmClient(profile.token)
    client.close
  }

}
