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

  private def unlearnBehaviorFor(regexString: String, client: SlackRtmClient, profile: SlackBotProfile, message: Message): Unit = {
    val action = try {
      for {
        triggers <- RegexMessageTriggerQueries.allMatching(regexString, profile.teamId)
        _ <- DBIO.sequence(triggers.map(_.behavior.unlearn(lambdaService)))
      } yield s"$regexString? Never heard of it."
    } catch {
      case e: AmazonServiceException => DBIO.successful("D'oh! That didn't work.")
    }
    val reply = models.runNow(action)
    val messageContext = SlackContext(client, profile, message)
    SlackMessageEvent(messageContext).context.sendMessage(reply)
  }

  def displayHelpFor(helpString: String, client: SlackRtmClient, profile: SlackBotProfile, message: Message): Unit = {
    val action = for {
      maybeTeam <- Team.find(profile.teamId)
      triggers <- maybeTeam.map { team =>
        RegexMessageTriggerQueries.allFor(team)
      }.getOrElse(DBIO.successful(Seq()))
    } yield {
        val triggerItemsString = triggers.map { ea =>
          s"\n• ${ea.regex.pattern.pattern()}"
        }.mkString("")
        s"""
           |Here's what I can do so far:$triggerItemsString
           |
           |To teach me something new:
           |
           |`@ellipsis: learn <regex with N capture groups> function(param1,...,paramN) { <code that returns result>; }`
           |""".stripMargin
      }
    val text = models.runNow(action)
    val messageContext = SlackContext(client, profile, message)
    SlackMessageEvent(messageContext).context.sendMessage(text)
  }

  def startFor(profile: SlackBotProfile) {

    val client = SlackRtmClient(profile.token, 5.seconds)
    clients += client
    val selfId = client.state.self.id

    val learnRegex = s"""<@$selfId>:\\s+learn\\s+(\\S+)\\s+(.+)""".r
    val unlearnRegex = s"""<@$selfId>:\\s+unlearn\\s+(\\S+)""".r
    val helpRegex = s"""<@$selfId>:\\s+help\\s*(\\S*)""".r

    client.onMessage { message =>
      if (message.user != selfId) {
        message.text match {
          case learnRegex(regexString, code) => learnBehaviorFor(regexString.r, code, client, profile, message)
          case unlearnRegex(regexString) => unlearnBehaviorFor(regexString, client, profile, message)
          case helpRegex(helpString) => displayHelpFor(helpString, client, profile, message)
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
