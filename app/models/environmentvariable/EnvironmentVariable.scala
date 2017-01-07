package models.environmentvariable

import java.time.ZonedDateTime

trait EnvironmentVariable {
  val name: String
  val value: String
  val createdAt: ZonedDateTime
}
