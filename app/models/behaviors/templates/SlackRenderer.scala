package models.behaviors.templates

import java.util.regex.Matcher

import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.node._
import play.api.Logger

class SlackRenderer(stringBuilder: StringBuilder) extends AbstractVisitor {
  def escapeControlEntities(text: String): String = {
    val ampersandsEscaped = text.replaceAll("&", "&amp;")
    try {
      """(<[^<>\s][^<>]+>|<|>)""".r.replaceAllIn(ampersandsEscaped, m => {
        val str = m.matched
        val replacement = if (str == null || str.isEmpty) {
          ""
        } else if (str.matches("<[@#].+>")) {
          str
        } else {
          str.replaceAll("<", "&lt;").replaceAll(">", "&gt;")
        }
        Matcher.quoteReplacement(replacement)
      })
    } catch {
      case e: Throwable => {
        Logger.error(
          s"""Error trying to escape entities. Giving up and returning original text: [
             |$text
             |]""".stripMargin, e)
        text
      }
    }
  }

  override def visit(blockQuote: BlockQuote) {
    var node = blockQuote.getFirstChild
    while (node != null) {
      node match {
        case p: Paragraph => {
          stringBuilder.append("\r> ")
          var child = p.getFirstChild
          child.accept(this)
          child = child.getNext
          while (child != null) {
            child match {
              case s: SoftLineBreak => stringBuilder.append("\r> ")
              case h: HardLineBreak => stringBuilder.append("\r> ")
              case _ => child.accept(this)
            }
            child = child.getNext
          }
          if (node.getNext != null) {
            stringBuilder.append("\r> ")
          }
        }
        case _ => {
          stringBuilder.append("\r> ")
          node.accept(this)
        }
      }
      node = node.getNext
    }
  }

  override def visit(bulletList: BulletList) {
    stringBuilder.append("\r")
    visitChildren(bulletList)
    stringBuilder.append("\r")
  }

  override def visit(code: Code) {
    stringBuilder.append(s"`${escapeControlEntities(code.getLiteral)}`")
    visitChildren(code)
  }

  override def visit(document: Document) {
    visitChildren(document)
  }

  override def visit(emphasis: Emphasis) {
    stringBuilder.append("_")
    visitChildren(emphasis)
    stringBuilder.append("_")
  }

  override def visit(fencedCodeBlock: FencedCodeBlock) {
    stringBuilder.append("```")
    stringBuilder.append(escapeControlEntities(fencedCodeBlock.getLiteral))
    visitChildren(fencedCodeBlock)
    stringBuilder.append("```")
  }

  override def visit(hardLineBreak: HardLineBreak) {
    stringBuilder.append("\r")
    visitChildren(hardLineBreak)
  }

  override def visit(heading: Heading) {
    stringBuilder.append("*")
    visitChildren(heading)
    stringBuilder.append("*\r\r")
  }

  override def visit(thematicBreak: ThematicBreak) {
    stringBuilder.append("\r─────\r")
    visitChildren(thematicBreak)
  }

  override def visit(html: HtmlInline) {
    stringBuilder.append(escapeControlEntities(html.getLiteral))
    visitChildren(html)
  }

  override def visit(htmlBlock: HtmlBlock) {
    visitChildren(htmlBlock)
  }

  override def visit(image: Image) {
    visitChildren(image)
  }

  override def visit(indentedCodeBlock: IndentedCodeBlock) {
    stringBuilder.append("\r```\r")
    stringBuilder.append(escapeControlEntities(indentedCodeBlock.getLiteral))
    visitChildren(indentedCodeBlock)
    stringBuilder.append("\r```\r")

  }

  def linkWithTitle(link: Link): Unit = {
    stringBuilder.append("<")
    stringBuilder.append(s"${link.getDestination}")
    stringBuilder.append("|")
    visitChildren(link)
    stringBuilder.append(">")
  }

  override def visit(link: Link) {
    link.getFirstChild match {
      case e: Text => {
        if (e.getLiteral == link.getDestination) {
          stringBuilder.append(s"<${link.getDestination}>")
        } else {
          linkWithTitle(link)
        }
      }
      case _ => linkWithTitle(link)
    }
  }

  override def visit(listItem: ListItem) {
    listItem.getParent match {
      case o: OrderedList => {
        var index = 1
        var node: Node = o.getFirstChild
        while(node != listItem && node != null) {
          index += 1
          node = node.getNext
        }
        stringBuilder.append(index.toString)
        stringBuilder.append(". ")
      }
      case _ => stringBuilder.append("• ")
    }
    visitChildren(listItem)
  }

  override def visit(orderedList: OrderedList) {
    stringBuilder.append("\r")
    visitChildren(orderedList)
    stringBuilder.append("\r")
  }

  override def visit(paragraph: Paragraph) {
    paragraph.getParent match {
      case li: ListItem =>
      case _ => stringBuilder.append("\r")
    }
    visitChildren(paragraph)
    stringBuilder.append("\r")
  }

  override def visit(softLineBreak: SoftLineBreak) {
    stringBuilder.append("\r")
    visitChildren(softLineBreak)
  }

  override def visit(strongEmphasis: StrongEmphasis) {
    stringBuilder.append("*")
    visitChildren(strongEmphasis)
    stringBuilder.append("*")
  }

  override def visit(text: Text) {
    /* HACK: any leftover formatting characters not parsed as Markdown get
       surrounded by soft hyphens to disable accidental formatting in Slack */
    val safeText = text.getLiteral.
      replaceAll("""(\S)([*_`~])(\s|$)""", "$1\u00AD$2\u00AD$3").
      replaceAll("""(\s|^)([*_`~])(\S)""", "$1\u00AD$2\u00AD$3")
    stringBuilder.append(escapeControlEntities(safeText))
    visitChildren(text)
  }

  override def visit(customBlock: CustomBlock) {
    visitChildren(customBlock)
  }

  override def visit(customNode: CustomNode) {
    customNode match {
      case strikeThrough: Strikethrough => {
        stringBuilder.append("~")
        visitChildren(strikeThrough)
        stringBuilder.append("~")
      }
      case _ => visitChildren(customNode)
    }
  }
}
