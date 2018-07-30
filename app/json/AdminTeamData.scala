package json

import java.time.OffsetDateTime

case class AdminTeamData(
                          id: String,
                          name: String,
                          timeZone: String,
                          createdAt: OffsetDateTime,
                          allowShortcutMention: Boolean,
                          lastInvocationDate: Option[OffsetDateTime]
                        ) extends Ordered[AdminTeamData] {
  def compare(that: AdminTeamData): Int = {
    (for {
      thisLastInvoked <- this.lastInvocationDate
      thatLastInvoked <- that.lastInvocationDate
    } yield {
      thisLastInvoked.compareTo(thatLastInvoked)
    }).getOrElse {
      if (this.lastInvocationDate.isDefined && that.lastInvocationDate.isEmpty) {
        1
      } else if (this.lastInvocationDate.isEmpty && that.lastInvocationDate.isDefined) {
        -1
      } else {
        this.createdAt.compareTo(that.createdAt)
      }
    }
  }
}
