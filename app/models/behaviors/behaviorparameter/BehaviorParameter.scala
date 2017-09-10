package models.behaviors.behaviorparameter

import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.conversations.ParamCollectionState
import models.behaviors.input.Input
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

case class BehaviorParameter(
                              id: String,
                              rank: Int,
                              input: Input,
                              behaviorVersion: BehaviorVersion
                            ) {

  val name = input.name
  val maybeQuestion = input.maybeQuestion
  val paramType = input.paramType

  def question: String = maybeQuestion.getOrElse(s"What is the value for `$name`?")

  def promptAction(
                    maybeValue: Option[String],
                    context: BehaviorParameterContext,
                    paramState: ParamCollectionState,
                    isReminding: Boolean
                  )(implicit ec: ExecutionContext): DBIO[String] = {
    paramType.promptForAction(maybeValue, context, paramState, isReminding)
  }

  def toRaw: RawBehaviorParameter = {
    RawBehaviorParameter(id, rank, Some(input.id), behaviorVersion.id)
  }
}
