import models.bots.templates._
import models.bots.templates.ast._
import org.scalatestplus.play.PlaySpec

class TemplateParserSpec extends PlaySpec {

  val parser = new TemplateParser

  def pathForSeq(segments: Seq[String]): Path = {
    val identifiers = segments.map { ea => Identifier(ea) }
    Path(identifiers)
  }

  def pathFor(segments: String*): Path = pathForSeq(segments)

  def substitutionFor(segments: String*): Substitution = Substitution(pathForSeq(segments))

  def parse(text: String): Block = {
    val result = parser.parseAll(parser.block, text)
    result.successful mustBe true
    result.get
  }

  "TemplateParser" should {

    "parse a substitution" in  {
      val result = parse("{successResult.foo.bar}")
      result mustBe Block(Seq(substitutionFor("successResult", "foo", "bar")))
    }

    "parse a mix of text & substitutions" in {
      val result = parse("foo: { successResult.foo } bar: {successResult.bar}")
      result mustBe Block(Seq(
        Text("foo: "),
        substitutionFor("successResult", "foo"),
        Text(" bar: "),
        substitutionFor("successResult", "bar")
      ))
    }

    "parse over multiple lines" in {
      val result = parse(
        """
          |foo: { successResult.foo }
          |
          |bar: {successResult.bar}""".stripMargin)
      result mustBe Block(Seq(
        Text("\nfoo: "),
        substitutionFor("successResult", "foo"),
        Text("\n\nbar: "),
        substitutionFor("successResult", "bar")
      ))
    }

    "parse an iteration" in {
      val result = parse(
        """
          |{for item in successResult.items}
          |1. {item}
          |{endfor}""".stripMargin)
      result mustBe Block(Seq(
        Text("\n"),
        Iteration(Identifier("item"), pathFor("successResult", "items"), Block(Seq(Text("\n1. "), substitutionFor("item"), Text("\n"))))))
    }

    "parse nested iterations" in {
      val result = parse(
        """
          |{for item in successResult.items}
          |{for ea in someOtherList}
          |1. {item} {ea}
          |{endfor}{endfor}""".stripMargin)
      result mustBe Block(Seq(
        Text("\n"),
        Iteration(
          Identifier("item"),
          pathFor("successResult", "items"),
          Block(Seq(
            Text("\n"),
            Iteration(
              Identifier("ea"),
              pathFor("someOtherList"),
              Block(Seq(
                Text("\n1. "),
                substitutionFor("item"),
                Text(" "),
                substitutionFor("ea"),
                Text("\n")))
            )
          ))
        )
      ))

    }

  }

}
