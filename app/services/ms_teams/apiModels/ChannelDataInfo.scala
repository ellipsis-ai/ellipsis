package services.ms_teams.apiModels

case class ChannelDataInfo(
                            clientActivityId: Option[String],
                            tenant: Option[TenantInfo],
                            channel: Option[ChannelDataChannel],
                            team: Option[ChannelDataTeam]
                          )
