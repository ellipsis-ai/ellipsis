package json

import models.Team

case class BehaviorCategory(
                             name: String,
                             description: String,
                             behaviorVersions: Seq[BehaviorVersionData]
                             ) extends Ordered[BehaviorCategory] {

  def copyForTeam(team: Team): BehaviorCategory = {
    copy(behaviorVersions = behaviorVersions.map(_.copyForTeam(team)))
  }

  def isMisc: Boolean = name == "Miscellaneous"

  import scala.math.Ordered.orderingToOrdered
  def compare(that: BehaviorCategory): Int = (this.isMisc, this.name) compare (that.isMisc, that.name)

}
