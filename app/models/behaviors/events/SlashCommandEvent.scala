package models.behaviors.events

import akka.actor.ActorSystem
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.behavior.Behavior
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.{ActionChoice, BehaviorResponse, DeveloperContext}
import models.team.Team
import play.api.Configuration
import services.{DataService, DefaultServices}
import utils.UploadFileSpec

import scala.concurrent.{ExecutionContext, Future}

case class SlashCommandEvent(
                              profile: SlackBotProfile,
                              userSlackTeamId: String,
                              channel: String,
                              user: String,
                              text: String
                            ) extends Event with SlackEvent {

  val eventType: EventType = EventType.chat

  val teamId: String = profile.teamId
  val userIdForContext: String = user

  lazy val maybeChannel = Some(channel)
  lazy val name: String = Conversation.SLACK_CONTEXT

  lazy val messageText: String = text
  lazy val invocationLogText: String = relevantMessageText

  val maybeThreadId: Option[String] = None
  val maybeOriginalEventType: Option[EventType] = None

  override val isResponseExpected: Boolean = true
  val includesBotMention: Boolean = true

  def maybeOngoingConversation(dataService: DataService)(implicit ec: ExecutionContext): Future[Option[Conversation]] = {
    Future.successful(None)
  }

  def allOngoingConversations(dataService: DataService): Future[Seq[Conversation]] = {
    Future.successful(Seq())
  }

  def allBehaviorResponsesFor(
                               maybeTeam: Option[Team],
                               maybeLimitToBehavior: Option[Behavior],
                               services: DefaultServices
                             )(implicit ec: ExecutionContext): Future[Seq[BehaviorResponse]] = {
    val dataService = services.dataService
    for {
      triggers <- (for {
        team <- maybeTeam
        channel <- maybeChannel
      } yield {
        dataService.behaviorGroupDeployments.allActiveTriggersFor(context, channel, team)
      }).getOrElse(Future.successful(Seq()))
      activatedTriggerLists <- Future.successful {
        triggers.
          filter(_.isActivatedBy(this)).
          groupBy(_.behaviorVersion).
          values.
          toSeq
      }
      activatedTriggerListsWithParamCounts <- Future.sequence(
        activatedTriggerLists.map { list =>
          Future.sequence(list.map { trigger =>
            for {
              params <- dataService.behaviorParameters.allFor(trigger.behaviorVersion)
            } yield {
              (trigger, trigger.invocationParamsFor(this, params).size)
            }
          })
        }
      )
      // we want to chose activated triggers with more params first
      activatedTriggers <- Future.successful(activatedTriggerListsWithParamCounts.flatMap { list =>
        list.
          sortBy { case(_, paramCount) => paramCount }.
          map { case(trigger, _) => trigger }.
          reverse.
          headOption
      })
      responses <- Future.sequence(activatedTriggers.map { trigger =>
        for {
          params <- dataService.behaviorParameters.allFor(trigger.behaviorVersion)
          response <- dataService.behaviorResponses.buildFor(
            this,
            trigger.behaviorVersion,
            trigger.invocationParamsFor(this, params),
            Some(trigger),
            None,
            None
          )
        } yield response
      })
    } yield responses
  }


  def messageUserDataList: Set[MessageUserData] = Set() // TODO: hm

  def withOriginalEventType(originalEventType: EventType, isUninterrupted: Boolean): Event = this // TODO: hm

  def sendMessage(
                   unformattedText: String,
                   forcePrivate: Boolean,
                   maybeShouldUnfurl: Option[Boolean],
                   maybeConversation: Option[Conversation],
                   attachmentGroups: Seq[MessageAttachmentGroup],
                   files: Seq[UploadFileSpec],
                   choices: Seq[ActionChoice],
                   developerContext: DeveloperContext,
                   services: DefaultServices,
                   configuration: Configuration
                 )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {

    // No-op; we use the response_url instead
    Future.successful(None)
  }

}
