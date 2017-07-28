package models.behaviors

import javax.inject.Inject

import models.behaviors.behavior.Behavior
import models.behaviors.behaviorparameter.BehaviorParameterContext
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import models.behaviors.triggers.messagetrigger.MessageTrigger
import models.team.Team
import services._
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BehaviorResponseServiceImpl @Inject() (
                                            services: DefaultServices,
                                            slackEventService: SlackEventService
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
            isValid <- DBIO.from(param.paramType.isValid(v, context))
            json <- DBIO.from(param.paramType.prepareForInvocation(v, context))
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
                      maybeActivatedTrigger: Option[MessageTrigger],
                      maybeConversation: Option[Conversation]
                    ): DBIO[BehaviorResponse] = {
    parametersWithValuesForAction(event, behaviorVersion, paramValues, maybeConversation).map { paramsWithValues =>
      BehaviorResponse(event, behaviorVersion, maybeConversation, paramsWithValues, maybeActivatedTrigger, services)
    }
  }

  def buildFor(
                event: Event,
                behaviorVersion: BehaviorVersion,
                paramValues: Map[String, String],
                maybeActivatedTrigger: Option[MessageTrigger],
                maybeConversation: Option[Conversation]
              ): Future[BehaviorResponse] = {
    dataService.run(buildForAction(event, behaviorVersion, paramValues, maybeActivatedTrigger, maybeConversation))
  }

  def allFor(
              event: Event,
              maybeTeam: Option[Team],
              maybeLimitToBehavior: Option[Behavior]
            ): Future[Seq[BehaviorResponse]] = {
    event.allBehaviorResponsesFor(maybeTeam, maybeLimitToBehavior, services)
  }

}
