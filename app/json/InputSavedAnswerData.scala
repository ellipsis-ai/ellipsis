package json

import models.accounts.user.User
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class InputSavedAnswerData(
                            inputId: String,
                            myValueString: Option[String],
                            userAnswerCount: Int
                          )

object InputSavedAnswerData {

  def maybeFor(inputId: String, behaviorGroupVersion: BehaviorGroupVersion, user: User, dataService: DataService): Future[Option[InputSavedAnswerData]] = {
    for {
      maybeInput <- dataService.inputs.findByInputId(inputId, behaviorGroupVersion)
      savedAnswers <- dataService.savedAnswers.allFor(inputId)
    } yield {
      maybeInput.map { input =>
        val maybeMySavedAnswer = if (input.isSavedForTeam) {
          savedAnswers.find(_.maybeUserId.isEmpty)
        } else {
          savedAnswers.find(_.maybeUserId.contains(user.id))
        }
        InputSavedAnswerData(
          input.inputId,
          maybeMySavedAnswer.map(_.valueString),
          savedAnswers.length
        )
      }
    }
  }

}
