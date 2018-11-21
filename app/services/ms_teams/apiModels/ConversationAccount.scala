package services.ms_teams.apiModels

case class ConversationAccount(
                                id: String,
                                isGroup: Option[Boolean],
                                name: Option[String],
                                conversationType: String
                              )
