package models.behaviors.testing

import akka.actor.ActorSystem
import models.accounts.user.User
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.TestEventContext
import services._

import scala.concurrent.{ExecutionContext, Future}

case class InvocationTester(
                            user: User,
                            behaviorVersion: BehaviorVersion,
                            paramValues: Map[String, String],
                            services: DefaultServices
                          ) {

  def run(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[InvocationTestReport] = {
    val dataService = services.dataService
    for {
      params <- dataService.behaviorParameters.allFor(behaviorVersion)
      event <- Future.successful {
        TestMessageEvent(TestEventContext(user, behaviorVersion.team), "", includesBotMention = true, maybeScheduled = None)
      }
      paramValueMaybes <- Future.successful {
        params.map { param =>
          (param, paramValues.get(param.name))
        }
      }

      missingParams <- Future.successful {
        paramValueMaybes.
          filter { case(param, v) => v.isEmpty }.
          map { case(param, v) => param }
      }

      missingSimpleTokens <- dataService.requiredSimpleTokenApis.missingFor(user, behaviorVersion.groupVersion)

      report <- if (missingParams.isEmpty && missingSimpleTokens.isEmpty) {
        val invocationParamValues = paramValueMaybes.zipWithIndex.map { case ((param, v), i) =>
          (AWSLambdaConstants.invocationParamFor(i), v.get)
        }.toMap
        for {
          parametersWithValues <- dataService.behaviorResponses.parametersWithValuesFor(event, behaviorVersion, invocationParamValues, None)
          result <- dataService.behaviorVersions.resultFor(behaviorVersion, parametersWithValues, event, None)
        } yield InvocationTestReport(behaviorVersion, Some(result), Seq(), Seq())
      } else {
        Future.successful(InvocationTestReport(behaviorVersion, None, missingParams, missingSimpleTokens))
      }
    } yield report
  }

}
