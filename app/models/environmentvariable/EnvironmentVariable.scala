package models.environmentvariable

import java.time.OffsetDateTime

trait EnvironmentVariable {
  val name: String
  val value: String
  val createdAt: OffsetDateTime
}
