package services

import javax.inject._
import com.amazonaws.AmazonServiceException
import models._
import models.accounts.{SlackBotProfileQueries, SlackBotProfile}
import models.bots._
import models.bots.conversations.{LearnBehaviorConversation, Conversation, ConversationQueries}
import play.api.i18n.MessagesApi
import play.api.inject.ApplicationLifecycle
import slack.models.Message
import slack.rtm.SlackRtmClient
import akka.actor.ActorSystem
import slick.driver.PostgresDriver.api._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.matching.Regex

@Singleton
class SlackService @Inject() (lambdaService: AWSLambdaService, appLifecycle: ApplicationLifecycle, models: Models, messages: MessagesApi) {

  implicit val system = ActorSystem("slack")
  implicit val ec = system.dispatcher

  val clients = scala.collection.mutable.ListBuffer.empty[SlackRtmClient]

  start

  appLifecycle.addStopHook { () =>
    Future.successful(stop)
  }

  private def runBehaviorsFor(client: SlackRtmClient, profile: SlackBotProfile, message: Message): DBIO[Unit] = {
    for {
      maybeTeam <- Team.find(profile.teamId)
      behaviorResponses <- maybeTeam.map { team =>
        val messageContext = SlackContext(client, profile, message)
        RegexMessageTriggerQueries.behaviorResponsesFor(SlackMessageEvent(messageContext), team)
      }.getOrElse(DBIO.successful(Seq()))
    } yield {
        behaviorResponses.foreach(_.run(lambdaService))
      }
  }

  private def learnBehaviorFor(regex: Regex, code: String, client: SlackRtmClient, profile: SlackBotProfile, message: Message): DBIO[Unit] = {
    val eventualReply = try {
      BehaviorQueries.learnFor(regex, code, profile.teamId, lambdaService).map { maybeBehavior =>
        maybeBehavior.map { behavior =>
          "OK, I think I've got it."
        }.getOrElse {
          messages("cant_find_team")
        }
      }
    } catch {
      case e: AmazonServiceException => DBIO.successful("D'oh! That didn't work.")
    }
    eventualReply.map { reply =>
      val messageContext = SlackContext(client, profile, message)
      SlackMessageEvent(messageContext).context.sendMessage(reply)
    }
  }

  private def unlearnBehaviorFor(regexString: String, client: SlackRtmClient, profile: SlackBotProfile, message: Message): DBIO[Unit] = {
    val eventualReply = try {
      for {
        triggers <- RegexMessageTriggerQueries.allMatching(regexString, profile.teamId)
        _ <- DBIO.sequence(triggers.map(_.behavior.unlearn(lambdaService)))
      } yield {
        s"$regexString? Never heard of it."
      }
    } catch {
      case e: AmazonServiceException => DBIO.successful("D'oh! That didn't work.")
    }
    eventualReply.map { reply =>
      val messageContext = SlackContext(client, profile, message)
      SlackMessageEvent(messageContext).context.sendMessage(reply)
    }
  }

  def displayHelpFor(helpString: String, client: SlackRtmClient, profile: SlackBotProfile, message: Message): DBIO[Unit] = {
    for {
      maybeTeam <- Team.find(profile.teamId)
      triggers <- maybeTeam.map { team =>
        RegexMessageTriggerQueries.allFor(team)
      }.getOrElse(DBIO.successful(Seq()))
    } yield {
        val triggerItemsString = triggers.map { ea =>
          s"\n• ${ea.regex.pattern.pattern()}"
        }.mkString("")
        val text = s"""
           |Here's what I can do so far:$triggerItemsString
           |
           |To teach me something new:
           |
           |`@ellipsis: learn <regex with N capture groups> function(param1,...,paramN) { <code that returns result>; }`
           |""".stripMargin
        val messageContext = SlackContext(client, profile, message)
        SlackMessageEvent(messageContext).context.sendMessage(text)
      }
  }

  def startLearnConversationFor(client: SlackRtmClient, profile: SlackBotProfile, message: Message): DBIO[Unit] = {
    val eventualReply = for {
      maybeTeam <- Team.find(profile.teamId)
      maybeBehavior <- maybeTeam.map { team =>
        BehaviorQueries.createFor(team).map(Some(_))
      }.getOrElse(DBIO.successful(None))
      maybeConversation <- maybeBehavior.map { behavior =>
        LearnBehaviorConversation.createFor(behavior, ConversationQueries.SLACK_CONTEXT, message.user).map(Some(_))
      }.getOrElse(DBIO.successful(None))
      reply <- maybeConversation.map { conversation =>
        conversation.replyFor(message.text, lambdaService)
      }.getOrElse(DBIO.successful(messages("cant_find_team")))
    } yield reply
    eventualReply.map { reply =>
      val messageContext = SlackContext(client, profile, message)
      SlackMessageEvent(messageContext).context.sendMessage(reply)
    }
  }

  def handleMessageFor(client: SlackRtmClient, profile: SlackBotProfile, message: Message, selfId: String): DBIO[Unit] = {
    val startLearnConversationRegex = s"""<@$selfId>:\\s+learn\\s*$$""".r
    val oneLineLearnRegex = s"""<@$selfId>:\\s+learn\\s+(\\S+)\\s+(.+)""".r
    val unlearnRegex = s"""<@$selfId>:\\s+unlearn\\s+(\\S+)""".r
    val helpRegex = s"""<@$selfId>:\\s+help\\s*(\\S*)""".r

    message.text match {
      case startLearnConversationRegex() => startLearnConversationFor(client, profile, message)
      case oneLineLearnRegex(regexString, code) => learnBehaviorFor(regexString.r, code, client, profile, message)
      case unlearnRegex(regexString) => unlearnBehaviorFor(regexString, client, profile, message)
      case helpRegex(helpString) => displayHelpFor(helpString, client, profile, message)
      case _ => runBehaviorsFor(client, profile, message)
    }
  }

  def handleMessageInConversation(conversation: Conversation, client: SlackRtmClient, profile: SlackBotProfile, message: Message, selfId: String): DBIO[Unit] = {
    conversation.replyFor(message.text, lambdaService).map { reply =>
      val messageContext = SlackContext(client, profile, message)
      SlackMessageEvent(messageContext).context.sendMessage(reply)
    }
  }

  def startFor(profile: SlackBotProfile) {

    val client = SlackRtmClient(profile.token, 5.seconds)
    clients += client
    val selfId = client.state.self.id

    client.onMessage { message =>
      if (message.user != selfId) {
        val action = ConversationQueries.findOngoingFor(message.user, ConversationQueries.SLACK_CONTEXT).flatMap { maybeConversation =>
         maybeConversation.map { conversation =>
           handleMessageInConversation(conversation, client, profile, message, selfId)
         }.getOrElse {
           handleMessageFor(client, profile, message, selfId)
         }
        }
        models.runNow(action)
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
