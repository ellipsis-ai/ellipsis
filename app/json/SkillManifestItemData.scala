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
    this.lastUsed.map { thisLastUsed =>
      that.lastUsed.map { thatLastUsed =>
        thisLastUsed.compareTo(thatLastUsed)
      }.getOrElse {
        1
      }
    }.getOrElse {
      if (that.lastUsed.isDefined) {
        -1
      } else {
        0
      }
    }
  }
}
