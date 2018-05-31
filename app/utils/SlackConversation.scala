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
  val isPublic: Boolean = !is_private.exists(identity) && !isGroup && !isIm
  val isArchived: Boolean = is_archived.exists(identity)

  val membersList: Seq[String] = members.map(_.toSeq).getOrElse(Seq())

  def visibleToUser(userId: String): Boolean = {
    isPublic || membersList.contains(userId)
  }

  val maybeMpimPurpose: Option[String] = {
    if (isMpim) {
      purpose.map(_.value)
    } else {
      None
    }
  }

  val computedName: String = maybeMpimPurpose.orElse(name).getOrElse("<unnamed channel>")

  def sortKey: String = {
    val kindPart = if (isPublic) { 1 } else if (isGroup) { 2 } else { 3 }
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
