package models.behaviors.savedanswer

import models.behaviors.input.Input
import org.joda.time.DateTime

case class SavedAnswer(
                      id: String,
                      input: Input,
                      valueString: String,
                      maybeUserId: Option[String],
                      createdAt: DateTime
                                  ) {
  def toRaw: RawSavedAnswer = {
    RawSavedAnswer(id, input.id, valueString, maybeUserId, createdAt)
  }
}
