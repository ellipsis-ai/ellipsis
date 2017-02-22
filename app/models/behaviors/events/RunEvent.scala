package models.behaviors.events

import akka.actor.ActorSystem
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.BehaviorResponse
import models.behaviors.behavior.Behavior
import models.behaviors.conversations.conversation.Conversation
import models.team.Team
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.libs.ws.WSClient
import services.{AWSLambdaConstants, AWSLambdaService, DataService}
import utils.SlackMessageSender

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RunEvent(
                     profile: SlackBotProfile,
                     behavior: Behavior,
                     paramValues: Map[String, String],
                     channel: String,
                     maybeThreadId: Option[String],
                     user: String,
                     ts: String
                  ) extends Event with SlackEvent {

  val messageText: String = ""
  val includesBotMention: Boolean = false
  val isResponseExpected: Boolean = false
  val invocationLogText: String = s"Running behavior ${behavior.id}"

  val teamId: String = behavior.team.id
  val userIdForContext: String = user

  lazy val maybeChannel = Some(channel)
  lazy val name: String = Conversation.SLACK_CONTEXT

  def allOngoingConversations(dataService: DataService): Future[Seq[Conversation]] = Future.successful(Seq())

  def sendMessage(
                   unformattedText: String,
                   forcePrivate: Boolean,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation],
                   maybeActions: Option[MessageActions] = None
                 )(implicit actorSystem: ActorSystem): Future[Option[String]] = {
    SlackMessageSender(
      client,
      user,
      unformattedText,
      forcePrivate,
      channel,
      channel,
      maybeThreadId,
      maybeShouldUnfurl,
      maybeConversation,
      maybeActions
    ).send
  }

  def allBehaviorResponsesFor(
                               maybeTeam: Option[Team],
                               maybeLimitToBehavior: Option[Behavior],
                               lambdaService: AWSLambdaService,
                               dataService: DataService,
                               cache: CacheApi,
                               ws: WSClient,
                               configuration: Configuration
                             ): Future[Seq[BehaviorResponse]] = {
    for {
      maybeBehaviorVersion <- dataService.behaviors.maybeCurrentVersionFor(behavior)
      responses <- maybeBehaviorVersion.map { behaviorVersion =>
        for {
          params <- dataService.behaviorParameters.allFor(behaviorVersion)
          invocationParams <- Future.successful(paramValues.flatMap { case(name, value) =>
            params.find(_.name == name).map { param =>
              (AWSLambdaConstants.invocationParamFor(param.rank - 1), value)
            }
          })
          response <- BehaviorResponse.buildFor(
            this,
            behaviorVersion,
            invocationParams,
            None,
            None,
            lambdaService,
            dataService,
            cache,
            ws,
            configuration
          )
        } yield Seq(response)
      }.getOrElse(Future.successful(Seq()))
    } yield responses
  }

}
