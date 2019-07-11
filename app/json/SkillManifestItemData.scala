package json

import java.time.OffsetDateTime

case class SkillManifestItemData(
                                  name: String,
                                  id: Option[String],
                                  editor: Option[UserData],
                                  description: String,
                                  managed: Boolean,
                                  created: OffsetDateTime,
                                  firstDeployed: Option[OffsetDateTime],
                                  lastUsed: Option[OffsetDateTime]
                                ) extends Ordered[SkillManifestItemData] {
  def compare(that: SkillManifestItemData): Int = {
    (this.lastUsed, that.lastUsed) match {
      case (Some(a), Some(b)) => a.compareTo(b)
      case (Some(_), None) => 1
      case (None, Some(_)) => -1
      case (None, None) => this.created.compareTo(that.created)
    }
  }
}
