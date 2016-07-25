package json

import models.Team

case class BehaviorCategory(
                             name: String,
                             description: String,
                             behaviorVersions: Seq[BehaviorVersionData]
                             ) {

  def copyForTeam(team: Team): BehaviorCategory = {
    copy(behaviorVersions = behaviorVersions.map(_.copyForTeam(team)))
  }

}
