package models.behaviors.savedanswer

import java.time.OffsetDateTime

import models.behaviors.input.Input

case class SavedAnswer(
                      id: String,
                      input: Input,
                      valueString: String,
                      maybeUserId: Option[String],
                      createdAt: OffsetDateTime
                                  ) {
  def toRaw: RawSavedAnswer = {
    RawSavedAnswer(id, input.id, valueString, maybeUserId, createdAt)
  }
}
