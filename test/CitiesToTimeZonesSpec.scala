import org.scalatestplus.play.PlaySpec
import utils.{CitiesToTimeZones, CityInfo}

class CitiesToTimeZonesSpec extends PlaySpec {

  val citiesToTimezones = new CitiesToTimeZones()

  "possibleTimeZonesFor" should {

    "find TZs for 'toronto'" in {
      val cities = citiesToTimezones.possibleCitiesFor("toronto")
      cities must contain(CityInfo("Toronto", "Toronto", "America/Toronto", 2600000))
    }

    "find TZs for 'san f'" in {
      val cities = citiesToTimezones.possibleCitiesFor("san f")
      cities must contain(CityInfo("San Francisco", "San Francisco", "America/Los_Angeles", 864816))
    }

    "find TZs for 'montreal', which needs to find ascii name" in {
      val cities = citiesToTimezones.possibleCitiesFor("montreal")
      cities must contain(CityInfo("Montréal", "Montreal", "America/Toronto", 1600000))
    }

  }

}
