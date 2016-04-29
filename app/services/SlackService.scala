package services

import javax.inject._
import com.amazonaws.AmazonServiceException
import models._
import models.accounts.{SlackBotProfileQueries, SlackBotProfile}
import models.bots._
import play.api.inject.ApplicationLifecycle
import slack.models.Message
import slack.rtm.SlackRtmClient
import akka.actor.ActorSystem
import slick.driver.PostgresDriver.api._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.matching.Regex

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

  private def learnBehaviorFor(regex: Regex, code: String, client: SlackRtmClient, profile: SlackBotProfile, message: Message): Unit = {
    val action = try {
      BehaviorQueries.learnFor(regex, code, profile.teamId, lambdaService).map { maybeBehavior =>
        maybeBehavior.map { behavior =>
          "OK, I think I've got it."
        }.getOrElse {
          "Couldn't find your team!?!"
        }
      }

    } catch {
      case e: AmazonServiceException => DBIO.successful("D'oh! That didn't work.")
    }
    val reply = models.runNow(action)
    val messageContext = SlackContext(client, profile, message)
    SlackMessageEvent(messageContext).context.sendMessage(reply)
  }

  def startFor(profile: SlackBotProfile) {

    val client = SlackRtmClient(profile.token, 5.seconds)
    clients += client
    val selfId = client.state.self.id

    client.onMessage { message =>
      if (message.user != selfId) {
        message.text match {
          case learnRegex(regexString, code) => learnBehaviorFor(regexString.r, code, client, profile, message)
          case _ => runBehaviorsFor(client, profile, message)
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
