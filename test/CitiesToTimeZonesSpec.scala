import org.scalatestplus.play.PlaySpec
import utils.{CitiesToTimeZones, CityInfo}

class CitiesToTimeZonesSpec extends PlaySpec {

  val citiesToTimezones = new CitiesToTimeZones()

  "possibleTimeZonesFor" should {

    "find TZs for 'toronto'" in {
      val cities = citiesToTimezones.possibleCitiesFor("toronto")
      cities must contain(CityInfo("Toronto", "America/Toronto", 2600000))
    }

    "find TZs for 'san f'" in {
      val cities = citiesToTimezones.possibleCitiesFor("san f")
      cities must contain(CityInfo("San Francisco", "America/Los_Angeles", 864816))
    }

  }

}
