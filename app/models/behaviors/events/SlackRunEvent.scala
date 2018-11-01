package models.behaviors.events

import akka.actor.ActorSystem
import models.behaviors.{BehaviorResponse, BotResult}
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorversion.BehaviorVersion
import models.team.Team
import services.{AWSLambdaConstants, DefaultServices}
import utils.SlackMessageReactionHandler

import scala.concurrent.{ExecutionContext, Future}

case class SlackRunEvent(
                           eventContext: SlackEventContext,
                           behaviorVersion: BehaviorVersion,
                           arguments: Map[String, String],
                           maybeOriginalEventType: Option[EventType],
                           override val isEphemeral: Boolean,
                           override val maybeResponseUrl: Option[String],
                           maybeTriggeringMessageTs: Option[String]
                        ) extends Event {

  override type EC = SlackEventContext

  val eventType: EventType = EventType.api
  def withOriginalEventType(originalEventType: EventType, isUninterrupted: Boolean): Event = {
    this.copy(maybeOriginalEventType = Some(originalEventType))
  }

  val messageText: String = ""
  val includesBotMention: Boolean = false
  def messageUserDataList: Set[MessageUserData] = Set.empty

  val isResponseExpected: Boolean = false
  val invocationLogText: String = s"Running behavior ${behaviorVersion.id}"

  def allBehaviorResponsesFor(
                               maybeTeam: Option[Team],
                               maybeLimitToBehavior: Option[Behavior],
                               services: DefaultServices
                             )(implicit ec: ExecutionContext): Future[Seq[BehaviorResponse]] = {
    val dataService = services.dataService
    for {
      params <- dataService.behaviorParameters.allFor(behaviorVersion)
      invocationParams <- Future.successful(arguments.flatMap { case(name, value) =>
        params.find(_.name == name).map { param =>
          (AWSLambdaConstants.invocationParamFor(param.rank - 1), value)
        }
      })
      response <- dataService.behaviorResponses.buildFor(
        this,
        behaviorVersion,
        invocationParams,
        None,
        None,
        None,
        userExpectsResponse = true
      )
    } yield Seq(response)
  }

  override def resultReactionHandler(eventualResults: Future[Seq[BotResult]], services: DefaultServices)
                                    (implicit ec: ExecutionContext, actorSystem: ActorSystem): Future[Seq[BotResult]] = {
    eventContext.reactionHandler(eventualResults, maybeTriggeringMessageTs, services)
  }

}
