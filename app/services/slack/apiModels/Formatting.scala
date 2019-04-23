package services.slack.apiModels

import play.api.libs.json._

object Formatting {

  lazy implicit val slackCommentFormat: Format[SlackComment] = Json.format[SlackComment]
  lazy implicit val slackFileFormat: Format[SlackFile] = Json.format[SlackFile]
  lazy implicit val actionSelectOptionFormat: Format[ActionSelectOption] = Json.format[ActionSelectOption]
  lazy implicit val confirmFieldFormat: Format[ConfirmField] = Json.format[ConfirmField]
  lazy implicit val actionFieldFormat: Format[ActionField] = Json.format[ActionField]
  lazy implicit val attachmentFieldFormat: Format[AttachmentField] = Json.format[AttachmentField]
  lazy implicit val attachmentFormat: Format[Attachment] = Json.format[Attachment]
  lazy implicit val slackUserProfileFormat: Format[SlackUserProfile] = Json.format[SlackUserProfile]
  lazy implicit val slackEnterpriseUserFormat: Format[SlackEnterpriseUser] = Json.format[SlackEnterpriseUser]
  lazy implicit val slackUserFormat: Format[SlackUser] = Json.format[SlackUser]
  lazy implicit val slackTeamFormat: Format[SlackTeam] = Json.format[SlackTeam]
  lazy implicit val membershipDataFormat: Format[MembershipData] = Json.format[MembershipData]

}
