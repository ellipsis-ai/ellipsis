package services.caching

import json.ActionChoiceData
import json.Formatting._
import models.IDs
import models.behaviors.behaviorversion.BehaviorResponseType
import models.behaviors.invocationlogentry.InvocationLogEntry
import models.behaviors.{DeveloperContext, ParameterValue, ParameterWithValue, SuccessResult}
import play.api.libs.json.{JsObject, JsValue}
import services.{AWSLambdaLogResult, DataService}

import scala.concurrent.{ExecutionContext, Future}

case class SuccessResultData(
                              eventKey: String,
                              behaviorVersionId: String,
                              maybeConversationId: Option[String],
                              result: JsValue,
                              payloadJson: JsValue,
                              parametersWithValues: Seq[ParameterWithValueData],
                              invocationJson: JsObject,
                              maybeResponseTemplate: Option[String],
                              maybeLogResult: Option[AWSLambdaLogResult],
                              responseType: String,
                              isForCopilot: Boolean,
                              developerContext: DeveloperContextData
                            )

object SuccessResultData {
  def cacheSuccessResult(invocationLogEntry: InvocationLogEntry, result: SuccessResult, cacheService: CacheService)
                        (implicit ec: ExecutionContext): Future[Unit] = {
    val eventKey = IDs.next
    val successResultData = SuccessResultData(
      eventKey,
      result.behaviorVersion.id,
      result.maybeConversation.map(_.id),
      result.result,
      result.payloadJson,
      result.parametersWithValues.map { pv =>
        ParameterWithValueData(pv.parameter.id, pv.invocationName, pv.maybeValue)
      },
      result.invocationJson,
      result.maybeResponseTemplate,
      result.maybeLogResult,
      result.responseType.id,
      result.isForCopilot,
      DeveloperContextData(
        result.developerContext.maybeBehaviorVersion.map(_.id),
        result.developerContext.isForUndeployedBehaviorVersion,
        result.developerContext.hasUndeployedBehaviorVersionForAuthor,
        result.developerContext.isInDevMode,
        result.developerContext.isInInvocationTester
      )
    )
    for {
      _ <- cacheService.cacheEvent(eventKey, result.event)
      _ <- cacheService.cacheSuccessResultDataForCopilot(invocationLogEntry.id, successResultData)
    } yield {}
  }

  def maybeSuccessResultFor(
                             invocationLogEntry: InvocationLogEntry,
                             dataService: DataService,
                             cacheService: CacheService
                           )(implicit ec: ExecutionContext): Future[Option[SuccessResult]] = {
    for {
      maybeResultData <- cacheService.getSuccessResultDataForCopilot(invocationLogEntry.id)
      maybeResult <- maybeResultData.map { resultData =>
        for {
          maybeEvent <- cacheService.getEvent(resultData.eventKey)
          maybeConversation <- resultData.maybeConversationId.map { convoId =>
            dataService.conversations.find(convoId)
          }.getOrElse(Future.successful(None))
          maybeBehaviorVersion <- dataService.behaviorVersions.findWithoutAccessCheck(resultData.behaviorVersionId)
          behaviorParameters <- maybeBehaviorVersion.map { bv =>
            dataService.behaviorParameters.allFor(bv)
          }.getOrElse(Future.successful(Seq.empty))
          maybeParametersWithValue <- Future.successful {
            val allMaybeValues = resultData.parametersWithValues.map { pwv =>
              behaviorParameters.find(ea => ea.id == pwv.behaviorParameterId).map { bp =>
                ParameterWithValue(bp, pwv.invocationName, pwv.maybeValue)
              }
            }
            Option(allMaybeValues).filter(_.forall(ea => ea.isDefined)).map(_.flatten)
          }
        } yield {
          for {
            event <- maybeEvent
            behaviorVersion <- maybeBehaviorVersion
            parametersWithValues <- maybeParametersWithValue
            responseType <- BehaviorResponseType.find(resultData.responseType)
          } yield {
            val developerContext = DeveloperContext(
              Some(behaviorVersion),
              resultData.developerContext.isForUndeployedBehaviorVersion,
              resultData.developerContext.hasUndeployedBehaviorVersionForAuthor,
              resultData.developerContext.isInDevMode,
              resultData.developerContext.isInInvocationTester
            )
            SuccessResult(
              event,
              behaviorVersion,
              maybeConversation,
              resultData.result,
              resultData.payloadJson,
              parametersWithValues,
              resultData.invocationJson,
              resultData.maybeResponseTemplate,
              resultData.maybeLogResult,
              responseType,
              resultData.isForCopilot,
              developerContext,
              dataService
            )
          }
        }
      }.getOrElse(Future.successful(None))
    } yield maybeResult
  }
}

case class ParameterWithValueData(
                                   behaviorParameterId: String,
                                   invocationName: String,
                                   maybeValue: Option[ParameterValue]
                                 )

case class DeveloperContextData(
                                 maybeBehaviorVersionId: Option[String],
                                 isForUndeployedBehaviorVersion: Boolean,
                                 hasUndeployedBehaviorVersionForAuthor: Boolean,
                                 isInDevMode: Boolean,
                                 isInInvocationTester: Boolean
                               )
