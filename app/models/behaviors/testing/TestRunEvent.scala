package models.behaviors.testing

import models.behaviors.BehaviorResponse
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.TestEventContext
import models.team.Team
import services.{AWSLambdaConstants, DefaultServices}

import scala.concurrent.{ExecutionContext, Future}

case class TestRunEvent(
                         eventContext: TestEventContext,
                         behaviorVersion: BehaviorVersion,
                         arguments: Map[String, String]
                       ) extends TestEvent {

  override type EC = TestEventContext

  val messageText: String = ""
  val includesBotMention: Boolean = false
  val invocationLogText: String = s"Running behavior for test: ${behaviorVersion.id}"
  val team = eventContext.team
  val user = eventContext.user

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
