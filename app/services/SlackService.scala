package services

import javax.inject._
import com.amazonaws.AmazonServiceException
import models._
import models.accounts.{OAuth2Token, SlackProfileQueries, SlackBotProfileQueries, SlackBotProfile}
import models.bots._
import models.bots.conversations.{Conversation, ConversationQueries}
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

  private def unlearnBehaviorFor(patternString: String, messageContext: SlackContext): DBIO[Unit] = {
    val eventualReply = try {
      for {
        triggers <- MessageTriggerQueries.allWithExactPattern(patternString, messageContext.profile.teamId)
        _ <- DBIO.sequence(triggers.map(_.behavior.unlearn(lambdaService)))
      } yield {
        s"$patternString? Never heard of it."
      }
    } catch {
      case e: AmazonServiceException => DBIO.successful("D'oh! That didn't work.")
    }
    eventualReply.map { reply =>
      SlackMessageEvent(messageContext).context.sendMessage(reply)
    }
  }

  private def helpStringFor(behaviors: Seq[Behavior], prompt: String, matchString: String): DBIO[String] = {
    DBIO.sequence(behaviors.map { ea =>
      MessageTriggerQueries.allFor(ea)
    }).map(_.flatten).map { triggersForBehaviors =>
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
      if (behaviorStrings.isEmpty) {
        ""
      } else {
        s"$prompt$matchString:${behaviorStrings.toSeq.sortBy(_.toLowerCase).mkString("")}"
      }
    }
  }

  def displayHelpFor(helpString: String, messageContext: SlackContext): DBIO[Unit] = {
    val maybeHelpSearch = Option(helpString).filter(_.trim.nonEmpty)
    for {
      maybeTeam <- Team.find(messageContext.profile.teamId)
      matchingTriggers <- maybeTeam.map { team =>
        maybeHelpSearch.map { helpSearch =>
          MessageTriggerQueries.allMatching(helpSearch, team)
        }.getOrElse {
          MessageTriggerQueries.allFor(team)
        }
      }.getOrElse(DBIO.successful(Seq()))
      behaviors <- DBIO.successful(matchingTriggers.map(_.behavior).distinct)
      (skills, knowledge) <- DBIO.successful(behaviors.partition(_.isSkill))
      matchString <- DBIO.successful(maybeHelpSearch.map { s =>
        s" that matches `$s`"
      }.getOrElse(""))
      skillsString <- helpStringFor(skills, "Here's what I can do", matchString)
      knowledgeString <- helpStringFor(knowledge, "Here's what I know", matchString)
    } yield {
        val text = s"""
           |$skillsString
           |
           |$knowledgeString
           |
           |To teach me something new, just type `@ellipsis: learn`
           |""".stripMargin
        SlackMessageEvent(messageContext).context.sendMessage(text)
      }
  }

  def startLearnConversationFor(messageContext: SlackContext): DBIO[Unit] = {
    val newBehaviorLink = lambdaService.configuration.getString("application.apiBaseUrl").map { baseUrl =>
      val path = controllers.routes.ApplicationController.newBehavior(messageContext.profile.teamId)
      s"$baseUrl$path"
    }.get
    messageContext.sendMessage(s"I love to learn. Come <$newBehaviorLink|teach me something new>.")
    DBIO.successful(Unit)
  }

  def startInvokeConversationFor(messageContext: SlackContext): DBIO[Unit] = {
    for {
      maybeTeam <- Team.find(messageContext.profile.teamId)
      behaviorResponses <- maybeTeam.map { team =>
        BehaviorResponse.allFor(SlackMessageEvent(messageContext), team)
      }.getOrElse(DBIO.successful(Seq()))
      maybeResponse <- DBIO.successful(behaviorResponses.headOption)
      _ <- maybeResponse.map { response =>
        response.run(lambdaService)
      }.getOrElse {
        if (messageContext.isResponseExpected) {
          messageContext.sendMessage(
            s"""
               |I don't know how to respond to `${messageContext.message.text}`
               |
               |Try `@ellipsis: help` to see what I can do.
             """.stripMargin)
        }
        DBIO.successful(Unit)
      }
    } yield Unit
  }

  def setEnvironmentVariable(name: String, value:String, messageContext: SlackContext): DBIO[Unit] = {
    for {
      maybeTeam <- Team.find(messageContext.profile.teamId)
      maybeEnvVar <- maybeTeam.map { team =>
        EnvironmentVariableQueries.ensureFor(name, value, team)
      }.getOrElse(DBIO.successful(None))
    } yield {
      SlackMessageEvent(messageContext).context.sendMessage(s"OK, saved $name!")
    }
  }

  def remember(messageContext: SlackContext): DBIO[Unit] = {
    val profile = messageContext.profile
    for {
      maybeTeam <- Team.find(profile.teamId)
      maybeOAuthToken <- OAuth2Token.maybeFullForSlackTeamId(profile.slackTeamId)
      maybeUserClient <- DBIO.successful(maybeOAuthToken.map { token =>
        SlackApiClient(token.accessToken)
      })
      maybeHistory <- maybeUserClient.map { userClient =>
        DBIO.from(userClient.getChannelHistory(messageContext.message.channel, latest = Some(messageContext.message.ts))).map(Some(_))
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
              MessageTriggerQueries.createFor(behavior, lastQuestion, requiresBotMention = false, shouldTreatAsRegex = false, isCaseSensitive = false)
            }.getOrElse {
              DBIO.successful(Unit)
            }.map(_ => behaviorWithContent)
          }
        }.map(Some(_)) transactionally
      }.getOrElse(DBIO.successful(None))
    } yield {
      maybeBehavior.foreach { behavior =>
        behavior.editLinkFor(lambdaService.configuration).foreach { link =>
          SlackMessageEvent(messageContext).context.sendMessage(s"OK, I compiled recent messages at $link")
        }
      }

    }
  }

  def handleMessageFor(messageContext: SlackContext): DBIO[Unit] = {

    val setEnvironmentVariableRegex = s"""^set\\s+env\\s+(\\S+)\\s+(.*)$$""".r
    val startLearnConversationRegex = s"""^learn\\s*$$""".r
    val unlearnRegex = s"""^unlearn\\s+(\\S+)""".r
    val helpRegex = s"""^help\\s*(\\S*.*)$$""".r
    val rememberRegex = s"""^(remember|\\^)\\s*$$""".r

    if (messageContext.includesBotMention) {
      messageContext.relevantMessageText match {
        case setEnvironmentVariableRegex(name, value) => setEnvironmentVariable(name, value, messageContext)
        case startLearnConversationRegex() => startLearnConversationFor(messageContext)
        case unlearnRegex(regexString) => unlearnBehaviorFor(regexString, messageContext)
        case helpRegex(helpString) => displayHelpFor(helpString, messageContext)
        case rememberRegex(cmd) => remember(messageContext)
        case _ => startInvokeConversationFor(messageContext)
      }
    } else {
      startInvokeConversationFor(messageContext)
    }
  }

  def handleMessageInConversation(conversation: Conversation, messageContext: SlackContext): DBIO[Unit] = {
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
        val messageContext = SlackContext(client, profile, message)
        val action = ConversationQueries.findOngoingFor(message.user, Conversation.SLACK_CONTEXT).flatMap { maybeConversation =>
         maybeConversation.map { conversation =>
           handleMessageInConversation(conversation, messageContext)
         }.getOrElse {
           handleMessageFor(messageContext)
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
