package models.behaviors.savedanswer

import models.behaviors.input.Input
import org.joda.time.LocalDateTime

case class SavedAnswer(
                      id: String,
                      input: Input,
                      valueString: String,
                      maybeUserId: Option[String],
                      createdAt: LocalDateTime
                                  ) {
  def toRaw: RawSavedAnswer = {
    RawSavedAnswer(id, input.id, valueString, maybeUserId, createdAt)
  }
}
