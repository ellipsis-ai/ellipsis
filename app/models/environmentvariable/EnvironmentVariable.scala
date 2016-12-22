package models.environmentvariable

import org.joda.time.DateTime

trait EnvironmentVariable {
  val name: String
  val value: String
  val createdAt: DateTime
}
