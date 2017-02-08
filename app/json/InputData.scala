package json

import models.behaviors.input.Input
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class InputData(
                      id: Option[String],
                      name: String,
                      paramType: Option[BehaviorParameterTypeData],
                      question: String,
                      isSavedForTeam: Boolean,
                      isSavedForUser: Boolean,
                      groupId: Option[String]
                    ) {

  val isShared = groupId.isDefined

  val maybeNonEmptyQuestion: Option[String] = Option(question).filter(_.nonEmpty)

}

object InputData {

  def fromInput(input: Input, dataService: DataService): Future[InputData] = {
    BehaviorParameterTypeData.from(input.paramType, dataService).map { paramTypeData =>
      InputData(
        Some(input.id),
        input.name,
        Some(paramTypeData),
        input.question,
        input.isSavedForTeam,
        input.isSavedForUser,
        input.maybeBehaviorGroup.map(_.id)
      )
    }

  }
}
