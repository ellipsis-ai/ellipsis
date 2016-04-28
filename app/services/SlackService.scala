package services

import javax.inject._
import models._
import models.accounts.{SlackBotProfileQueries, SlackBotProfile}
import models.bots.{SlackContext, SlackMessageEvent, RegexMessageTriggerQueries}
import play.api.inject.ApplicationLifecycle
import slack.models.Message
import slack.rtm.SlackRtmClient
import akka.actor.ActorSystem
import slick.driver.PostgresDriver.api._
import scala.concurrent.Future
import scala.concurrent.duration._

@Singleton
class SlackService @Inject() (lambdaService: AWSLambdaService, appLifecycle: ApplicationLifecycle, models: Models) {

  implicit val system = ActorSystem("slack")
  implicit val ec = system.dispatcher

  val clients = scala.collection.mutable.ListBuffer.empty[SlackRtmClient]

  start

  appLifecycle.addStopHook { () =>
    Future.successful(stop)
  }

  private def runBehaviorsFor(client: SlackRtmClient, profile: SlackBotProfile, message: Message): Unit = {
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

  val learnRegex = """.*\s+learn\s+(\S+)\s+(.+)$""".r


  def startFor(profile: SlackBotProfile) {

    val client = SlackRtmClient(profile.token, 5.seconds)
    clients += client
    val selfId = client.state.self.id

    client.onMessage { message =>
      if (message.user != selfId) {
        learnRegex.findFirstMatchIn(message.text).map { learnMatch =>
          val messageContext = SlackContext(client, profile, message)
          SlackMessageEvent(messageContext).context.sendMessage(s"learn match: ${learnMatch.subgroups.mkString(",")}")
        }.getOrElse {
          runBehaviorsFor(client, profile, message)
        }
      }
    }

  }

  private def allProfiles: DBIO[Seq[SlackBotProfile]] = {
    SlackBotProfileQueries.all.result
  }

  def start: Future[Any] = {
    stop
    val action = allProfiles.map { profiles =>
      profiles.foreach { profile =>
        startFor(profile)
      }
    }
    models.run(action)
  }

  def stop = clients.foreach { ea =>
    ea.close
    clients -= ea
  }

}
