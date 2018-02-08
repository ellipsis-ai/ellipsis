package models.loggedevent

case class ChannelDetails(
                         medium: Option[String],
                         channel: Option[String],
                         members: Seq[String]
                         )
