import org.scalatestplus.play.PlaySpec
import utils.ShellEscaping

class ShellEscapingSpec extends PlaySpec {

  "escapeWithSingleQuotes" should {
    "add single quotes and escape any included single quotes" in {
      ShellEscaping.escapeWithSingleQuotes("""I'm "cool", don't you think?""") mustBe """'I'\''m "cool", don'\''t you think?'"""
    }
  }
}
