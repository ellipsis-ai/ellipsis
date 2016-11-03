package models.behaviors.testing

import models.accounts.user.User
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.BehaviorResponse
import play.api.Configuration
import play.api.cache.CacheApi
import services.{AWSLambdaConstants, AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class InvocationTester(
                            user: User,
                            behaviorVersion: BehaviorVersion,
                            paramValues: Map[String, String],
                            lambdaService: AWSLambdaService,
                            dataService: DataService,
                            cache: CacheApi,
                            configuration: Configuration
                          ) {

  def run: Future[InvocationTestReport] = {
    for {
      params <- dataService.behaviorParameters.allFor(behaviorVersion)
      event <- Future.successful {
        val context = TestMessageContext(user, behaviorVersion.team, "", includesBotMention = true)
        TestEvent(context)
      }
      paramValueMaybes <- Future.successful {
        params.map { param =>
          (param, paramValues.get(param.name))
        }.toMap
      }

      missingParams <- Future.successful {
        paramValueMaybes.
          filter { case(param, v) => v.isEmpty }.
          map { case(param, v) => param }.
          toSeq
      }
      report <- if (missingParams.isEmpty) {
        val invocationParamValues = paramValueMaybes.zipWithIndex.map { case ((param, v), i) =>
          (AWSLambdaConstants.invocationParamFor(i), v.get)
        }
        for {
          parametersWithValues <- BehaviorResponse.parametersWithValuesFor(event, behaviorVersion, invocationParamValues, None, dataService, cache, configuration)
          result <- dataService.behaviorVersions.resultFor(behaviorVersion, parametersWithValues, event)
        } yield InvocationTestReport(behaviorVersion, Some(result), Seq())
      } else {
        Future.successful(InvocationTestReport(behaviorVersion, None, missingParams))
      }
    } yield report
  }

}
