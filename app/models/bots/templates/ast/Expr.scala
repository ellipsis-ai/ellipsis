package models.bots.templates.ast

import models.bots.templates.MarkdownRenderer
import play.api.libs.json.JsLookupResult

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
