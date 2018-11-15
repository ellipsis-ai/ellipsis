package models.behaviors.events

trait MessageActionsGroup extends MessageAttachmentGroup {

  type ActionType <: MessageAction
  val id: String
  val actions: Seq[ActionType]
  val maybeText: Option[String]
  val maybeUserDataList: Option[Set[MessageUserData]]
  val maybeColor: Option[String]
  val maybeTitle: Option[String]
  val maybeTitleLink: Option[String]

}
