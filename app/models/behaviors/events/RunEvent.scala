package models.behaviors.events

import models.behaviors.BehaviorResponse
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorversion.BehaviorVersion
import models.team.Team
import services.{AWSLambdaConstants, DefaultServices}

import scala.concurrent.{ExecutionContext, Future}

trait RunEvent extends Event {

  val behaviorVersion: BehaviorVersion
  val arguments: Map[String, String]

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

}
