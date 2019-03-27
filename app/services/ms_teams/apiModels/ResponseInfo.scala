package services.ms_teams.apiModels

import org.commonmark.node._
import org.commonmark.parser.Parser

import scala.collection.mutable

case class ResponseInfo(
                         `type`: String,
                         from: MessageParticipantInfo,
                         conversation: ConversationAccount,
                         recipient: Option[MessageParticipantInfo],
                         text: Option[String],
                         textFormat: Option[String],
                         replyToId: Option[String],
                         attachments: Option[Seq[Attachment]],
                         entities: Option[Seq[MentionEntity]]
                       )

object ResponseInfo {

  def newForMessage(
                     from: MessageParticipantInfo,
                     conversation: ConversationAccount,
                     maybeRecipient: Option[MessageParticipantInfo],
                     text: String,
                     textFormat: String,
                     maybeReplyToId: Option[String],
                     attachments: Option[Seq[Attachment]],
                     members: Seq[MSTeamsUser]
                   ): ResponseInfo = {
    val node = Parser.builder().build().parse(text)
    val collector = new MentionCollector()
    node.accept(collector)
    val mentionStrings = collector.mentionStrings
    val entities = members.flatMap(_.maybeMentionEntity).flatMap{ ea =>
      if (mentionStrings.contains(ea.text)) {
        Some(ea)
      } else {
        None
      }
    }
    ResponseInfo(
      "message",
      from,
      conversation,
      maybeRecipient,
      Some(text),
      Some(textFormat),
      maybeReplyToId,
      attachments,
      Some(entities)
    )
  }

  def newForTyping(
                    from: MessageParticipantInfo,
                    conversation: ConversationAccount,
                    recipient: MessageParticipantInfo
                  ): ResponseInfo = {
    ResponseInfo(
      "typing",
      from,
      conversation,
      None,
      None,
      None,
      None,
      None,
      None
    )
  }

}

class MentionCollector extends AbstractVisitor {

  val builder: mutable.ArrayBuilder[String] = mutable.ArrayBuilder.make[String]

  def mentionStrings: Set[String] = {
    builder.result.toSet[String]
  }

  override def visit(code: Code) {
    // do nothing and ignore mentions in code blocks
  }

  override def visit(fencedCodeBlock: FencedCodeBlock) {
    // do nothing and ignore mentions in fenced code blocks
  }

  override def visit(html: HtmlInline) {
    if (html.getLiteral == "<at>") {
      html.getNext match {
        case n: Text => {
          builder += s"<at>${n.getLiteral}</at>"
        }
      }
    } else {
      super.visit(html)
    }
  }

  override def visit(indentedCodeBlock: IndentedCodeBlock) {
    // do nothing and ignore mentions in indented code blocks
  }

  def linkWithTitle(link: Link): Unit = {
    // do nothing with links
  }

  override def visit(link: Link) {
    // do nothing with links
  }

}
