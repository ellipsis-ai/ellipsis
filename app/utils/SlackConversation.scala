package utils

case class SlackConversationTopic(
                                   value: String,
                                   creator: String,
                                   last_set: Long
                                 )

case class SlackConversationPurpose(
                                     value: String,
                                     creator: String,
                                     last_set: Long
                                   )

case class SlackConversation(
                              id: String,
                              name: Option[String],
                              is_channel: Option[Boolean],
                              is_group: Option[Boolean],
                              is_im: Option[Boolean],
                              created: Option[Long],
                              creator: Option[String],
                              is_archived: Option[Boolean],
                              is_general: Option[Boolean],
                              name_normalized: Option[String],
                              is_shared: Option[Boolean],
                              is_ext_shared: Option[Boolean],
                              is_org_shared: Option[Boolean],
                              is_member: Option[Boolean],
                              is_private: Option[Boolean],
                              is_mpim: Option[Boolean],
                              latest: Option[String],
                              topic: Option[SlackConversationTopic],
                              purpose: Option[SlackConversationPurpose],
                              num_members: Option[Long],
                              members: Option[Array[String]],
                              locale: Option[String]
                            ) {

  val isGroup: Boolean = is_group.exists(identity)
  val isIm: Boolean = is_im.exists(identity)
  val isMpim: Boolean = is_mpim.exists(identity)
  val isPrivateChannel: Boolean = !isMpim && !isIm && is_private.exists(identity)
  val isPublic: Boolean = !isPrivateChannel && !isGroup && !isIm && !isMpim
  val isArchived: Boolean = is_archived.exists(identity)
  val isShared: Boolean = is_ext_shared.exists(identity)

  val membersList: Seq[String] = members.map(_.toSeq).getOrElse(Seq())

  val isBotMember: Boolean = is_member.exists(identity)

  def isVisibleToUserWhere(isPrivateMember: Boolean, forceAdmin: Boolean): Boolean = {
    isPublic || (isPrivateMember || forceAdmin)
  }

  val maybeImPurpose: Option[String] = {
    if (isMpim || isIm) {
      purpose.map(_.value)
    } else {
      None
    }
  }

  val computedName: String = maybeImPurpose.orElse(name).getOrElse("<unnamed channel>")

  def sortKey: String = {
    val kindPart = if (isPublic) { 1 } else if (isPrivateChannel) { 2 } else if (isMpim) { 3 } else { 4 }
    s"$kindPart-$computedName"
  }

}

object SlackConversation {

  def defaultFor(id: String, name: String): SlackConversation = SlackConversation(
    id,
    Some(name),
    is_channel = None,
    is_group = None,
    is_im = None,
    created = None,
    creator = None,
    is_archived = None,
    is_general = None,
    name_normalized = None,
    is_shared = None,
    is_ext_shared = None,
    is_org_shared = None,
    is_member = None,
    is_private = None,
    is_mpim = None,
    latest = None,
    topic = None,
    purpose = None,
    num_members = None,
    members = None,
    locale = None
  )

}
