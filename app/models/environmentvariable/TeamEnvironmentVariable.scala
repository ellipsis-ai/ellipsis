package models.environmentvariable

import org.joda.time.DateTime
import models.team.Team

case class TeamEnvironmentVariable(
                                    name: String,
                                    value: String,
                                    team: Team,
                                    createdAt: DateTime
                                  ) extends EnvironmentVariable {
  def toRaw: RawTeamEnvironmentVariable = {
    RawTeamEnvironmentVariable(name, value, team.id, createdAt)
  }
}
