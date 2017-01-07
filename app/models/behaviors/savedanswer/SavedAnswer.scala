package models.behaviors.savedanswer

import java.time.ZonedDateTime

import models.behaviors.input.Input

case class SavedAnswer(
                      id: String,
                      input: Input,
                      valueString: String,
                      maybeUserId: Option[String],
                      createdAt: ZonedDateTime
                                  ) {
  def toRaw: RawSavedAnswer = {
    RawSavedAnswer(id, input.id, valueString, maybeUserId, createdAt)
  }
}
