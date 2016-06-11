package utils

import scala.util.parsing.combinator.{JavaTokenParsers, RegexParsers}

sealed trait Expr
case class Block(children: Seq[Expr])
case class Text(value: String) extends Expr
case class Identifier(name: String) extends Expr
case class Path(segments: Seq[Identifier]) extends Expr
case class Substitution(path: Path) extends Expr
case class Iteration(item: Identifier, list: Path, body: Block) extends Expr

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

}
