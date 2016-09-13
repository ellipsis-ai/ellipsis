package models.bots.templates

import models.bots.templates.ast._

import scala.util.parsing.combinator.{JavaTokenParsers, RegexParsers}

class TemplateParser extends RegexParsers with JavaTokenParsers {

  override def skipWhitespace = false

  val reserved: Parser[String] = "endfor"

  def text: Parser[Text] = """[^\{]+""".r ^^ { s => Text(s) }

  def identifier: Parser[Identifier] = not(reserved) ~> ident ^^ { s => Identifier(s) }

  def path: Parser[Path] = repsep(identifier, ".") ^^ { segments => Path(segments) }

  def substitution: Parser[Substitution] = """\{\s*""".r ~> path <~ """\s*\}""".r ^^ { path => Substitution(path) }

  def block: Parser[Block] = rep(text | substitution | iteration) ^^ { children => Block(children) }

  def iteration: Parser[Iteration] =
    """\{\s*for\s+""".r ~> identifier ~ """\s+in\s+""".r ~ path ~ """\s*\}""".r ~ block <~ """\{\s*endfor\s*\}""".r ^^ {
    case itemIdentifier ~ _ ~ listPath ~ _ ~ block =>
      Iteration(itemIdentifier, listPath, block)
  }

  def parseBlockFrom(text: String): ParseResult[Block] = parse(block, text)

}
