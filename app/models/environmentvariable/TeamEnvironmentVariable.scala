package models.environmentvariable

import org.joda.time.LocalDateTime
import models.team.Team

case class TeamEnvironmentVariable(
                                    name: String,
                                    value: String,
                                    team: Team,
                                    createdAt: LocalDateTime
                                  ) extends EnvironmentVariable {
  def toRaw: RawTeamEnvironmentVariable = {
    RawTeamEnvironmentVariable(name, value, team.id, createdAt)
  }
}
