package models.environmentvariable

import org.joda.time.LocalDateTime

trait EnvironmentVariable {
  val name: String
  val value: String
  val createdAt: LocalDateTime
}
