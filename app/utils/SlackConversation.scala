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

case class SlackConversationLatestInfo(
                                        `type`: Option[String],
                                        user: Option[String],
                                        text: Option[String],
                                        ts: Option[String]
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
                              is_read_only: Option[Boolean],
                              is_mpim: Option[Boolean],
                              latest: Option[SlackConversationLatestInfo],
                              topic: Option[SlackConversationTopic],
                              purpose: Option[SlackConversationPurpose],
                              num_members: Option[Long],
                              locale: Option[String]
                            ) {

  val isGroup: Boolean = is_group.exists(identity)
  val isIm: Boolean = is_im.exists(identity)
  val isMpim: Boolean = is_mpim.exists(identity)
  val isPrivateChannel: Boolean = !isMpim && !isIm && is_private.exists(identity)
  val isPublic: Boolean = !isPrivateChannel && !isGroup && !isIm && !isMpim
  val isArchived: Boolean = is_archived.exists(identity)
  val isExternallyShared: Boolean = is_ext_shared.exists(identity)
  val isOrgShared: Boolean = is_org_shared.exists(identity)
  val isReadOnly: Boolean = is_read_only.exists(identity)

  val isBotMember: Boolean = is_member.exists(identity)

  def isVisibleToUserWhere(isPrivateMember: Boolean, isAdmin: Boolean): Boolean = {
    isPublic || isPrivateMember || isAdmin
  }

  val maybeImPurpose: Option[String] = {
    if (isMpim || isIm) {
      purpose.map(_.value)
    } else {
      None
    }
  }

  val computedName: String = {
    maybeImPurpose.
      orElse(name).
      orElse {
        if(isIm) {
          Some("Direct message")
        } else {
          None
        }
      }.
      getOrElse("<unnamed channel>")
  }

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
    is_read_only = None,
    is_mpim = None,
    latest = None,
    topic = None,
    purpose = None,
    num_members = None,
    locale = None
  )

}
