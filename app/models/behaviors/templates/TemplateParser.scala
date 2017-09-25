package models.behaviors.templates

import models.behaviors.templates.ast._

import scala.util.parsing.combinator.{JavaTokenParsers, RegexParsers}

class TemplateParser extends RegexParsers with JavaTokenParsers {

  override def skipWhitespace = false

  val reserved: Parser[String] = "endfor" | "endif" | "else"

  def text: Parser[Text] = """[^\{]+""".r ^^ { s => Text(s) }

  def identifier: Parser[Identifier] = not(reserved) ~> ident ^^ { s => Identifier(s) }

  def path: Parser[Path] = repsep(identifier, ".") ^^ { segments => Path(segments) }

  def substitution: Parser[Substitution] = """\{\s*""".r ~> path <~ """\s*\}""".r ^^ { path => Substitution(path) }

  def block: Parser[Block] = rep(text | substitution | iteration | conditional) ^^ { children => Block(children) }

  def iteration: Parser[Iteration] =
    """\{\s*for\s+""".r ~> identifier ~ """\s+in\s+""".r ~ path ~ """\s*\}[ \t]*\n?""".r ~ block <~ """\{\s*endfor\s*\}[ \t]*\n?""".r ^^ {
    case itemIdentifier ~ _ ~ listPath ~ _ ~ block =>
      Iteration(itemIdentifier, listPath, block)
  }

  def conditional: Parser[Conditional] = """\{\s*if\s+""".r ~> path ~ """\s*\}[ \t]*\n?""".r ~ block ~ (("""\{\s*else\s*\}\s*\n?""".r ~> block)?) <~ """\{\s*endif\s*\}[ \t]*\n?""".r ^^ {
    case condition ~ _ ~ block ~ maybeElseBlock => Conditional(condition, block, maybeElseBlock)
  }

  def parseBlockFrom(text: String): ParseResult[Block] = parse(block, text)

}
