import models.behaviors.templates._
import models.behaviors.templates.ast._
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
        Iteration(Identifier("item"), pathFor("successResult", "items"), Block(Seq(Text("1. "), substitutionFor("item"), Text("\n"))))))
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
            Iteration(
              Identifier("ea"),
              pathFor("someOtherList"),
              Block(Seq(
                Text("1. "),
                substitutionFor("item"),
                Text(" "),
                substitutionFor("ea"),
                Text("\n")))
            )
          ))
        )
      ))

    }

    "parse a conditional" in {
      val result = parse("{if foo.isBar}foo is a bar{endif}")
      result mustBe Block(Seq(
        Conditional(pathFor("foo", "isBar"), Block(Seq(Text("foo is a bar"))), None))
      )
    }

    "parse nested conditionals" in {
      val result = parse("{if foo.isBar}{if shouldDisplay}foo is a bar{endif}{endif}")
      result mustBe Block(Seq(
        Conditional(
          pathFor("foo", "isBar"),
          Block(Seq(
            Conditional(
              pathFor("shouldDisplay"),
              Block(Seq(
                Text("foo is a bar")
              )),
              None
            )
          )),
          None
        ))
      )
    }

    "parse a conditional with an else clause" in {
      val result = parse("{if foo.isBar}foo is a bar{else}foo is not a bar{endif}")
      result mustBe Block(Seq(
        Conditional(
          pathFor("foo", "isBar"),
          Block(Seq(Text("foo is a bar"))),
          Some(Block(Seq(Text("foo is not a bar"))))
        ))
      )
    }

  }

}
