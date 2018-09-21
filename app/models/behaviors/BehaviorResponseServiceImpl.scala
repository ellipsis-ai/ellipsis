package models.behaviors

import akka.actor.ActorSystem
import javax.inject.Inject
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorparameter.BehaviorParameterContext
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.conversations.parentconversation.NewParentConversation
import models.behaviors.events.Event
import models.behaviors.triggers.Trigger
import models.team.Team
import services._
import services.slack.SlackEventService
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

class BehaviorResponseServiceImpl @Inject() (
                                              services: DefaultServices,
                                              slackEventService: SlackEventService,
                                              implicit val ec: ExecutionContext,
                                              implicit val actorSystem: ActorSystem
                                        ) extends BehaviorResponseService {

  val dataService = services.dataService

  def parametersWithValuesForAction(
                                     event: Event,
                                     behaviorVersion: BehaviorVersion,
                                     paramValues: Map[String, String],
                                     maybeConversation: Option[Conversation]
                                   ): DBIO[Seq[ParameterWithValue]] = {
    for {
      params <- dataService.behaviorParameters.allForAction(behaviorVersion)
      invocationNames <- DBIO.successful(params.zipWithIndex.map { case (p, i) =>
        AWSLambdaConstants.invocationParamFor(i)
      })
      values <- DBIO.sequence(params.zip(invocationNames).map { case(param, invocationName) =>
        val context = BehaviorParameterContext(event, maybeConversation, param, services)
        paramValues.get(invocationName).map { v =>
          for {
            isValid <- param.paramType.isValidAction(v, context)
            json <- param.paramType.prepareForInvocation(v, context)
          } yield {
            Some(ParameterValue(v, json, isValid))
          }
        }.getOrElse(DBIO.successful(None))
      })
    } yield params.zip(values).zip(invocationNames).map { case((param, maybeValue), invocationName) =>
      ParameterWithValue(param, invocationName, maybeValue)
    }
  }

  def parametersWithValuesFor(
                               event: Event,
                               behaviorVersion: BehaviorVersion,
                               paramValues: Map[String, String],
                               maybeConversation: Option[Conversation]
                             ): Future[Seq[ParameterWithValue]] = {
    dataService.run(parametersWithValuesForAction(event, behaviorVersion, paramValues, maybeConversation))
  }

  def buildForAction(
                      event: Event,
                      behaviorVersion: BehaviorVersion,
                      paramValues: Map[String, String],
                      maybeActivatedTrigger: Option[Trigger],
                      maybeConversation: Option[Conversation],
                      maybeNewParent: Option[NewParentConversation],
                      userExpectsResponse: Boolean
                    ): DBIO[BehaviorResponse] = {
    parametersWithValuesForAction(event, behaviorVersion, paramValues, maybeConversation).map { paramsWithValues =>
      BehaviorResponse(event, behaviorVersion, maybeConversation, paramsWithValues, maybeActivatedTrigger, maybeNewParent, userExpectsResponse, services)
    }
  }

  def buildFor(
                event: Event,
                behaviorVersion: BehaviorVersion,
                paramValues: Map[String, String],
                maybeActivatedTrigger: Option[Trigger],
                maybeConversation: Option[Conversation],
                maybeNewParent: Option[NewParentConversation],
                userExpectsResponse: Boolean
              ): Future[BehaviorResponse] = {
    dataService.run(buildForAction(event, behaviorVersion, paramValues, maybeActivatedTrigger, maybeConversation, maybeNewParent, userExpectsResponse))
  }

  def allFor(
              event: Event,
              maybeTeam: Option[Team],
              maybeLimitToBehavior: Option[Behavior]
            ): Future[Seq[BehaviorResponse]] = {
    event.allBehaviorResponsesFor(maybeTeam, maybeLimitToBehavior, services)
  }

}
