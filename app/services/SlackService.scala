package services

import javax.inject._
import com.amazonaws.AmazonServiceException
import models._
import models.accounts.{OAuth2Token, SlackProfileQueries, SlackBotProfileQueries, SlackBotProfile}
import models.bots._
import models.bots.conversations.{LearnBehaviorConversation, Conversation, ConversationQueries}
import models.bots.triggers.MessageTriggerQueries
import play.api.i18n.MessagesApi
import play.api.inject.ApplicationLifecycle
import slack.api.SlackApiClient
import slack.models.Message
import slack.rtm.SlackRtmClient
import akka.actor.ActorSystem
import slick.driver.PostgresDriver.api._
import utils.QuestionAnswerExtractor
import scala.concurrent.Future
import scala.concurrent.duration._

@Singleton
class SlackService @Inject() (
                               lambdaService: AWSLambdaService,
                               appLifecycle: ApplicationLifecycle,
                               val models: Models,
                               messages: MessagesApi
                               ) {

  implicit val system = ActorSystem("slack")
  implicit val ec = system.dispatcher

  val clients = scala.collection.mutable.Map.empty[SlackBotProfile, SlackRtmClient]

  start

  appLifecycle.addStopHook { () =>
    Future.successful(stop)
  }

  private def runBehaviorsFor(client: SlackRtmClient, profile: SlackBotProfile, message: Message): DBIO[Unit] = {
    for {
      maybeTeam <- Team.find(profile.teamId)
      behaviorResponses <- maybeTeam.map { team =>
        val messageContext = SlackContext(client, profile, message)
        BehaviorResponse.allFor(SlackMessageEvent(messageContext), team)
      }.getOrElse(DBIO.successful(Seq()))
      _ <- DBIO.sequence(behaviorResponses.map(_.run(lambdaService)))
    } yield Unit
  }

  private def unlearnBehaviorFor(patternString: String, client: SlackRtmClient, profile: SlackBotProfile, message: Message): DBIO[Unit] = {
    val eventualReply = try {
      for {
        triggers <- MessageTriggerQueries.allMatching(patternString, profile.teamId)
        _ <- DBIO.sequence(triggers.map(_.behavior.unlearn(lambdaService)))
      } yield {
        s"$patternString? Never heard of it."
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
    val maybeHelpSearch = Option(helpString).filter(_.trim.nonEmpty)
    for {
      maybeTeam <- Team.find(profile.teamId)
      matchingTriggers <- maybeTeam.map { team =>
        maybeHelpSearch.map { helpSearch =>
          MessageTriggerQueries.allMatching(helpSearch, team.id)
        }.getOrElse {
          MessageTriggerQueries.allFor(team)
        }
      }.getOrElse(DBIO.successful(Seq()))
      behaviors <- DBIO.successful(matchingTriggers.map(_.behavior).distinct)
      triggersForBehaviors <- DBIO.sequence(behaviors.map { ea =>
        MessageTriggerQueries.allFor(ea)
      }).map(_.flatten)
    } yield {
        val grouped = triggersForBehaviors.groupBy(_.behavior)
        val behaviorStrings = grouped.map { case(behavior, triggers) =>
          val triggersString = triggers.map { ea =>
            s"`${ea.pattern}`"
          }.mkString(" or ")
          val editLink = behavior.editLinkFor(lambdaService.configuration).map { link =>
            s" <$link|Details>"
          }.getOrElse("")
          s"\n• $triggersString $editLink"
        }
        val behaviorsString = behaviorStrings.toSeq.sortBy(_.toLowerCase).mkString("")
        val matchString = maybeHelpSearch.map { s =>
          s" that matches `$s`"
        }.getOrElse("")
        val text = s"""
           |Here's what I respond to$matchString:$behaviorsString
           |
           |To teach me something new, just type `@ellipsis: learn`
           |""".stripMargin
        val messageContext = SlackContext(client, profile, message)
        SlackMessageEvent(messageContext).context.sendMessage(text)
      }
  }

  def startLearnConversationFor(client: SlackRtmClient, profile: SlackBotProfile, message: Message): DBIO[Unit] = {
    val newBehaviorLink = lambdaService.configuration.getString("application.apiBaseUrl").map { baseUrl =>
      val path = controllers.routes.ApplicationController.newBehavior(profile.teamId)
      s"$baseUrl$path"
    }.get
    val messageContext = SlackContext(client, profile, message)
    messageContext.sendMessage(s"I love to learn. Come <$newBehaviorLink|teach me something new>.")
    DBIO.successful(Unit)
    // TODO: decide whether to keep any of the learn behavior conversation
//    for {
//      maybeTeam <- Team.find(profile.teamId)
//      maybeBehavior <- maybeTeam.map { team =>
//        BehaviorQueries.createFor(team).map(Some(_))
//      }.getOrElse(DBIO.successful(None))
//      maybeConversation <- maybeBehavior.map { behavior =>
//        LearnBehaviorConversation.createFor(behavior, Conversation.SLACK_CONTEXT, message.user).map(Some(_))
//      }.getOrElse(DBIO.successful(None))
//      messageContext <- DBIO.successful(SlackContext(client, profile, message))
//      event <- DBIO.successful(SlackMessageEvent(messageContext))
//      _ <- maybeConversation.map { conversation =>
//        conversation.replyFor(event, lambdaService)
//      }.getOrElse(DBIO.successful(messageContext.sendMessage(messages("cant_find_team"))))
//    } yield Unit
  }

  def startInvokeConversationFor(client: SlackRtmClient, profile: SlackBotProfile, message: Message): DBIO[Unit] = {
    for {
      maybeTeam <- Team.find(profile.teamId)
      behaviorResponses <- maybeTeam.map { team =>
        val messageContext = SlackContext(client, profile, message)
        BehaviorResponse.allFor(SlackMessageEvent(messageContext), team)
      }.getOrElse(DBIO.successful(Seq()))
      maybeResponse <- DBIO.successful(behaviorResponses.headOption)
      _ <- maybeResponse.map { response =>
        response.run(lambdaService)
      }.getOrElse(DBIO.successful(Unit))
    } yield Unit
  }

  def setEnvironmentVariable(name: String, value:String, client: SlackRtmClient, profile: SlackBotProfile, message: Message): DBIO[Unit] = {
    for {
      maybeTeam <- Team.find(profile.teamId)
      maybeEnvVar <- maybeTeam.map { team =>
        EnvironmentVariableQueries.ensureFor(name, value, team)
      }.getOrElse(DBIO.successful(None))
    } yield {
      val messageContext = SlackContext(client, profile, message)
      SlackMessageEvent(messageContext).context.sendMessage(s"OK, saved $name!")
    }
  }

  def remember(client: SlackRtmClient, profile: SlackBotProfile, message: Message): DBIO[Unit] = {
    for {
      maybeTeam <- Team.find(profile.teamId)
      maybeSlackProfile <- SlackProfileQueries.allFor(profile.slackTeamId).map(_.headOption)
      maybeOAuthToken <- maybeSlackProfile.map { profile =>
        OAuth2Token.findByLoginInfo(profile.loginInfo)
      }.getOrElse(DBIO.successful(None))
      maybeUserClient <- DBIO.successful(maybeOAuthToken.map { token =>
        SlackApiClient(token.accessToken)
      })
      maybeHistory <- maybeUserClient.map { userClient =>
        DBIO.from(userClient.getChannelHistory(message.channel, latest = Some(message.ts))).map(Some(_))
      }.getOrElse(DBIO.successful(None))
      messages <- DBIO.successful(maybeHistory.map { history =>
        history.messages.slice(0, 10).reverse.flatMap { json =>
          (json \ "text").asOpt[String]
        }
      }.getOrElse(Seq()))
      qaExtractor <- DBIO.successful(QuestionAnswerExtractor(messages))
      maybeBehavior <- maybeTeam.map { team =>
        BehaviorQueries.createFor(team).flatMap { behavior =>
          behavior.copy(maybeResponseTemplate = Some(qaExtractor.possibleAnswerContent)).save.flatMap { behaviorWithContent =>
            qaExtractor.maybeLastQuestion.map { lastQuestion =>
              MessageTriggerQueries.ensureFor(behavior, lastQuestion)
            }.getOrElse {
              DBIO.successful()
            }.map(_ => behaviorWithContent)
          }
        }.map(Some(_)) transactionally
      }.getOrElse(DBIO.successful(None))
    } yield {
      maybeBehavior.foreach { behavior =>
        val messageContext = SlackContext(client, profile, message)
        behavior.editLinkFor(lambdaService.configuration).foreach { link =>
          SlackMessageEvent(messageContext).context.sendMessage(s"OK, I compiled recent messages at $link")
        }
      }

    }
  }

  def handleMessageFor(client: SlackRtmClient, profile: SlackBotProfile, message: Message, selfId: String): DBIO[Unit] = {
    val setEnvironmentVariableRegex = s"""<@$selfId>:\\s+set\\s+env\\s+(\\S+)\\s+(.*)$$""".r
    val startLearnConversationRegex = s"""<@$selfId>:\\s+learn\\s*$$""".r
    val unlearnRegex = s"""<@$selfId>:\\s+unlearn\\s+(\\S+)""".r
    val helpRegex = s"""<@$selfId>:\\s+help\\s*(\\S*.*)$$""".r
    val rememberRegex = s"""<@$selfId>:\\s+(remember|\\^)\\s*$$""".r

    message.text match {
      case setEnvironmentVariableRegex(name, value) => setEnvironmentVariable(name, value, client, profile, message)
      case startLearnConversationRegex() => startLearnConversationFor(client, profile, message)
      case unlearnRegex(regexString) => unlearnBehaviorFor(regexString, client, profile, message)
      case helpRegex(helpString) => displayHelpFor(helpString, client, profile, message)
      case rememberRegex(cmd) => remember(client, profile, message)
      case _ => startInvokeConversationFor(client, profile, message)
    }
  }

  def handleMessageInConversation(conversation: Conversation, client: SlackRtmClient, profile: SlackBotProfile, message: Message, selfId: String): DBIO[Unit] = {
    val messageContext = SlackContext(client, profile, message)
    conversation.replyFor(SlackMessageEvent(messageContext), lambdaService)
  }

  def stopFor(profile: SlackBotProfile): Unit = {

    println(s"stopping client for ${profile.userId}")

    clients.get(profile).foreach { client =>
      client.close()
    }
    clients.remove(profile)
  }

  def startFor(profile: SlackBotProfile) {

    stopFor(profile)

    println(s"starting client for ${profile.userId}")
    val client = SlackRtmClient(profile.token, 5.seconds)
    clients.put(profile, client)
    val selfId = client.state.self.id

    client.onMessage { message =>
      if (message.user != selfId) {
        val action = ConversationQueries.findOngoingFor(message.user, Conversation.SLACK_CONTEXT).flatMap { maybeConversation =>
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

  def stop = {
    clients.clone().foreach { case(profile, client) =>
      client.close
      clients.remove(profile)
    }
  }

}
