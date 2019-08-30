package models.behaviors.behaviorparameter

import akka.actor.ActorSystem
import models.behaviors.BotResult
import models.behaviors.conversations.ParamCollectionState
import models.behaviors.input.Input
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

case class BehaviorParameter(
                              id: String,
                              rank: Int,
                              input: Input,
                              behaviorVersionId: String
                            ) {

  val name = input.name
  val maybeQuestion = input.maybeQuestion
  val paramType = input.paramType

  def question: String = maybeQuestion.getOrElse(s"What is the value for `$name`?")

  def promptResultAction(
                    maybeValue: Option[String],
                    context: BehaviorParameterContext,
                    paramState: ParamCollectionState,
                    isReminding: Boolean
                  )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult] = {
    paramType.promptResultForAction(maybeValue, context, paramState, isReminding)
  }

  def toRaw: RawBehaviorParameter = {
    RawBehaviorParameter(id, rank, Some(input.id), behaviorVersionId)
  }
}
