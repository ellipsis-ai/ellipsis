package services.ms_teams.apiModels

import models.behaviors.ActionChoice

case class ActivityInfo(
                         activityType: String,
                         id: String,
                         timestamp: String,
                         serviceUrl: String,
                         channelId: String,
                         from: MessageParticipantInfo,
                         conversation: ConversationInfo,
                         recipient: MessageParticipantInfo,
                         textFormat: Option[String],
                         text: Option[String],
                         maybeActionChoice: Option[ActionChoice],
                         channelData: ChannelDataInfo
                       ) {
  val responseUrl: String = s"$serviceUrl/v3/conversations/${conversation.id}/activities/${id}"

  val maybeTenantId: Option[String] = channelData.tenant.map(_.id)
}
