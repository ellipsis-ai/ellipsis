package models.behaviors.events.slack

import json.UserData
import models.{IDs, SlackMessageFormatter}
import models.behaviors.events.MessageAttachment
import services.slack.apiModels.{ActionsBlock, Attachment, Block, MarkdownText, SectionBlock}
import utils.Color

case class SlackMessageAttachment(
                                   maybeText: Option[String] = None,
                                   maybeUserDataList: Option[Set[UserData]] = None,
                                   maybeTitle: Option[String] = None,
                                   maybeTitleLink: Option[String] = None,
                                   maybeColor: Option[Color] = None,
                                   maybeCallbackId: Option[String] = None,
                                   actions: Seq[SlackMessageAction] = Seq()
                                 ) extends MessageAttachment {
  private val maybeTitleBlock = maybeTitle.map { title =>
    val text = maybeTitleLink.map { link =>
      s"**[${title}](${link})**"
    }.getOrElse(s"**${title}**")
    SectionBlock(MarkdownText(text))
  }
  private val maybeTextBlock = maybeText.map { text =>
    val bodyText = SlackMessageFormatter.bodyTextFor(text, maybeUserDataList.getOrElse(Set.empty[UserData]))
    SectionBlock(MarkdownText(bodyText))
  }
  private val maybeActionsBlock = if (actions.nonEmpty) {
    Some(ActionsBlock(actions.map(_.actionField), maybeCallbackId.map(_ + IDs.next).getOrElse(IDs.next)))
  } else {
    None
  }
  val underlying: Seq[Block] = Seq(maybeTitleBlock, maybeTextBlock, maybeActionsBlock).flatten
//  val underlying = ActionsBlock(
//    fallback = Some("This feature is unavailable on this platform."),
//    callback_id = maybeCallbackId,
//    color = maybeColor.map(_.hexCode),
//    pretext = None,
//    author_name = None,
//    author_link = None,
//    author_icon = None,
//    title = maybeTitle,
//    title_link = maybeTitleLink,
//    text = maybeText.map(text => SlackMessageFormatter.bodyTextFor(text, maybeUserDataList.getOrElse(Set.empty[UserData]))),
//    fields = Seq(),
//    image_url = None,
//    thumb_url = None,
//    actions = actions.map(_.actionField),
//    mrkdwn_in = Seq("text")
//  )
}
