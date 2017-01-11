package models.behaviors.templates

import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.node._

class SlackRenderer(stringBuilder: StringBuilder) extends AbstractVisitor {
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
              case text: Text =>
              case _ => stringBuilder.append(" ")
            }
            child.accept(this)
            child = child.getNext
          }
          stringBuilder.append("\r> ")
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
    stringBuilder.append(s"`${code.getLiteral}`")
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
    stringBuilder.append(fencedCodeBlock.getLiteral)
    visitChildren(fencedCodeBlock)
    stringBuilder.append("```")
  }

  override def visit(hardLineBreak: HardLineBreak) {
    stringBuilder.append("\r")
    visitChildren(hardLineBreak)
  }

  override def visit(heading: Heading) {
    visitChildren(heading)
    stringBuilder.append("\r\r")
  }

  override def visit(thematicBreak: ThematicBreak) {
    visitChildren(thematicBreak)
  }

  override def visit(html: HtmlInline) {
    stringBuilder.append(html.getLiteral)
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
    stringBuilder.append(indentedCodeBlock.getLiteral)
    visitChildren(indentedCodeBlock)
    stringBuilder.append("\r```\r")

  }

  override def visit(link: Link) {
    link.getFirstChild match {
      case e: Text => {
        if (e.getLiteral == link.getDestination) {
          stringBuilder.append(s"<${link.getDestination}>")
        } else {
          stringBuilder.append("<")
          stringBuilder.append(s"${link.getDestination}")
          stringBuilder.append("|")
          visitChildren(link)
          stringBuilder.append(">")
        }
      }
      case _ => visitChildren(link)
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
        stringBuilder.append(") ")
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
    visitChildren(softLineBreak)
  }

  override def visit(strongEmphasis: StrongEmphasis) {
    stringBuilder.append("*")
    visitChildren(strongEmphasis)
    stringBuilder.append("*")
  }

  override def visit(text: Text) {
    stringBuilder.append(text.getLiteral)
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
