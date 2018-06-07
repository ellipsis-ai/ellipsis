package services.slack.apiModels

case class SlackUserProfile (
                              avatar_hash: Option[String],
                              status_text: Option[String],
                              status_emoji: Option[String],
                              first_name: Option[String],
                              last_name: Option[String],
                              real_name: Option[String],
                              display_name: Option[String],
                              real_name_normalized: Option[String],
                              display_name_normalized: Option[String],
                              email: Option[String],
                              skype: Option[String],
                              phone: Option[String],
                              image_24: String,
                              image_32: String,
                              image_48: String,
                              image_72: String,
                              image_192: String,
                              image_512: String,
                              team: Option[String]
                            )

case class SlackUser (
                       id: String,
                       team_id: Option[String],
                       name: String,
                       deleted: Option[Boolean],
                       color: Option[String],
                       profile: Option[SlackUserProfile],
                       is_bot: Option[Boolean],
                       is_admin: Option[Boolean],
                       is_owner: Option[Boolean],
                       is_primary_owner: Option[Boolean],
                       is_restricted: Option[Boolean],
                       is_ultra_restricted: Option[Boolean],
                       is_stranger: Option[Boolean],
                       has_2fa: Option[Boolean],
                       has_files: Option[Boolean],
                       tz: Option[String],
                       tz_offset: Option[Int],
                       presence: Option[String]
                     )
