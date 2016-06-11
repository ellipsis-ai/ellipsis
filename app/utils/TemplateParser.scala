package utils

import play.api.libs.json.JsLookupResult
import renderers.MarkdownRenderer

import scala.util.parsing.combinator.{JavaTokenParsers, RegexParsers}

sealed trait Expr {

  def accept(visitor: MarkdownRenderer): Unit = Unit

}

case class Block(children: Seq[Expr]) {

  def accept(visitor: MarkdownRenderer): Unit = {
    visitor.visit(this)
  }

}

case class Text(value: String) extends Expr {

  override def accept(visitor: MarkdownRenderer): Unit = {
    visitor.visit(this)
  }

}

case class Identifier(name: String) extends Expr

case class Path(segments: Seq[Identifier]) extends Expr {

  def dotString: String = segments.map(_.name).mkString(".")

}

case class Substitution(path: Path) extends Expr{

  override def accept(visitor: MarkdownRenderer): Unit = {
    visitor.visit(this)
  }

  def pathString: String = path.dotString

  def getFrom(result: JsLookupResult, segmentsRemaining: Seq[Identifier]): JsLookupResult = {
    segmentsRemaining.headOption.map { segment =>
      getFrom(result \ segment.name, segmentsRemaining.tail)
    }.getOrElse {
      result
    }
  }

  def nameToLookUpInEnvironment: String = {
    segmentsWithoutSuccessResult.headOption.map(_.name).getOrElse("")
  }

  def pathSegmentsToLookUpInEnvironment: Seq[String] = {
    segmentsWithoutSuccessResult.tail.map(_.name)
  }

  def hasSuccessResult: Boolean = path.segments.headOption.exists(_.name == "successResult")
  def segmentsWithoutSuccessResult: Seq[Identifier] = if (hasSuccessResult) path.segments.tail else path.segments

  def getFrom(result: JsLookupResult): JsLookupResult = getFrom(result, segmentsWithoutSuccessResult)
}

case class Iteration(item: Identifier, list: Path, body: Block) extends Expr {

  override def accept(visitor: MarkdownRenderer): Unit = {
    visitor.visit(this)
  }

}

class TemplateParser extends RegexParsers with JavaTokenParsers {

  override def skipWhitespace = false

  val reserved: Parser[String] = "endfor"

  def text: Parser[Text] = """[^\{]+""".r ^^ { s => Text(s) }

  def identifier: Parser[Identifier] = not(reserved) ~> ident ^^ { s => Identifier(s) }

  def path: Parser[Path] = repsep(identifier, ".") ^^ { segments => Path(segments) }

  def substitution: Parser[Substitution] = """\{\s*""".r ~> path <~ """\s*\}""".r ^^ { case path => Substitution(path) }

  def block: Parser[Block] = rep(text | substitution | iteration) ^^ { children => Block(children) }

  def iteration: Parser[Iteration] =
    """\{\s*for\s+""".r ~> identifier ~ """\s+in\s+""".r ~ path ~ """\s*\}""".r ~ block <~ """\{\s*endfor\s*\}""".r ^^ {
    case itemIdentifier ~ _ ~ listPath ~ _ ~ block =>
      Iteration(itemIdentifier, listPath, block)
  }

  def parseBlockFrom(text: String): ParseResult[Block] = parse(block, text)

}
