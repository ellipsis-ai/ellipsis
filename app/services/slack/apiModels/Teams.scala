package services.slack.apiModels

case class SlackTeam (
                        id: Option[String],
                        name: Option[String],
                        domain: Option[String],
                        email_domain: Option[String],
                        enterprise_id: Option[String],
                        enterprise_name: Option[String]
                      )
