import org.scalatestplus.play.PlaySpec
import utils.CitiesToTimeZones

class CitiesToTimeZonesSpec extends PlaySpec {

  val citiesToTimezones = new CitiesToTimeZones()

  "possibleTimeZonesFor" should {

    "find TZs for 'toronto'" in {
      val timeZones = citiesToTimezones.possibleTimeZonesFor("toronto")
      timeZones must contain("America/Toronto")
    }

    "find TZs for 'san f'" in {
      val timeZones = citiesToTimezones.possibleTimeZonesFor("san f")
      timeZones must contain("America/Los_Angeles")
    }

  }

}
