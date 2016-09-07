package models.environmentvariable

import org.joda.time.DateTime
import models.team.Team

case class EnvironmentVariable(
                                name: String,
                                value: String,
                                team: Team,
                                createdAt: DateTime
                              ) {
  def toRaw: RawEnvironmentVariable = {
    RawEnvironmentVariable(name, value, team.id, createdAt)
  }
}
