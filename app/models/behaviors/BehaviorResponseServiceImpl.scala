package models.behaviors

import javax.inject.Inject

import akka.actor.ActorSystem
import models.behaviors.behaviorparameter.BehaviorParameterContext
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import models.behaviors.triggers.messagetrigger.MessageTrigger
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.libs.ws.WSClient
import services.{AWSLambdaConstants, AWSLambdaService, DataService}
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BehaviorResponseServiceImpl @Inject() (
                                          dataService: DataService
                                        ) extends BehaviorResponseService {

  def parametersWithValuesForAction(
                                     event: Event,
                                     behaviorVersion: BehaviorVersion,
                                     paramValues: Map[String, String],
                                     maybeConversation: Option[Conversation],
                                     dataService: DataService,
                                     cache: CacheApi,
                                     configuration: Configuration,
                                     actorSystem: ActorSystem
                                   ): DBIO[Seq[ParameterWithValue]] = {
    for {
      params <- dataService.behaviorParameters.allForAction(behaviorVersion)
      invocationNames <- DBIO.successful(params.zipWithIndex.map { case (p, i) =>
        AWSLambdaConstants.invocationParamFor(i)
      })
      values <- DBIO.sequence(params.zip(invocationNames).map { case(param, invocationName) =>
        val context = BehaviorParameterContext(event, maybeConversation, param, cache, dataService, configuration, actorSystem)
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
                               maybeConversation: Option[Conversation],
                               dataService: DataService,
                               cache: CacheApi,
                               configuration: Configuration,
                               actorSystem: ActorSystem
                             ): Future[Seq[ParameterWithValue]] = {
    dataService.run(parametersWithValuesForAction(event, behaviorVersion, paramValues, maybeConversation, dataService, cache, configuration, actorSystem))
  }

  def buildForAction(
                      event: Event,
                      behaviorVersion: BehaviorVersion,
                      paramValues: Map[String, String],
                      maybeActivatedTrigger: Option[MessageTrigger],
                      maybeConversation: Option[Conversation],
                      lambdaService: AWSLambdaService,
                      dataService: DataService,
                      cache: CacheApi,
                      ws: WSClient,
                      configuration: Configuration,
                      actorSystem: ActorSystem
                    ): DBIO[BehaviorResponse] = {
    parametersWithValuesForAction(event, behaviorVersion, paramValues, maybeConversation, dataService, cache, configuration, actorSystem).map { paramsWithValues =>
      BehaviorResponse(event, behaviorVersion, maybeConversation, paramsWithValues, maybeActivatedTrigger, lambdaService, dataService, cache, ws, configuration)
    }
  }

  def buildFor(
                event: Event,
                behaviorVersion: BehaviorVersion,
                paramValues: Map[String, String],
                maybeActivatedTrigger: Option[MessageTrigger],
                maybeConversation: Option[Conversation],
                lambdaService: AWSLambdaService,
                dataService: DataService,
                cache: CacheApi,
                ws: WSClient,
                configuration: Configuration,
                actorSystem: ActorSystem
              ): Future[BehaviorResponse] = {
    dataService.run(buildForAction(event, behaviorVersion, paramValues, maybeActivatedTrigger, maybeConversation, lambdaService, dataService, cache, ws, configuration, actorSystem))
  }

}
