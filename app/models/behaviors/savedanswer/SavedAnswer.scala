package models.behaviors.savedanswer

import java.time.OffsetDateTime

case class SavedAnswer(
                      id: String,
                      inputId: String,
                      valueString: String,
                      maybeUserId: Option[String],
                      createdAt: OffsetDateTime
                      )
