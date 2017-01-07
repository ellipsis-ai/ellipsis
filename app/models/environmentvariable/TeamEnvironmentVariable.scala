package models.environmentvariable

import java.time.OffsetDateTime

import models.team.Team

case class TeamEnvironmentVariable(
                                    name: String,
                                    value: String,
                                    team: Team,
                                    createdAt: OffsetDateTime
                                  ) extends EnvironmentVariable {
  def toRaw: RawTeamEnvironmentVariable = {
    RawTeamEnvironmentVariable(name, value, team.id, createdAt)
  }
}
