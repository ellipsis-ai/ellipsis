package models.environmentvariable

import java.time.ZonedDateTime

import models.team.Team

case class TeamEnvironmentVariable(
                                    name: String,
                                    value: String,
                                    team: Team,
                                    createdAt: ZonedDateTime
                                  ) extends EnvironmentVariable {
  def toRaw: RawTeamEnvironmentVariable = {
    RawTeamEnvironmentVariable(name, value, team.id, createdAt)
  }
}
