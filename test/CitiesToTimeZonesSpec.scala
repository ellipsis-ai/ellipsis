import org.scalatestplus.play.PlaySpec
import utils.{CitiesToTimeZones, CityInfo}

class CitiesToTimeZonesSpec extends PlaySpec {

  val citiesToTimezones = new CitiesToTimeZones()

  val toronto = CityInfo("Toronto", "Toronto", Some("Ontario"), "Canada", "America/Toronto", 2600000, "Eastern Time")
  val sf = CityInfo("San Francisco", "San Francisco", Some("California"), "United States", "America/Los_Angeles", 864816, "Pacific Time")
  val montreal = CityInfo("Montréal", "Montreal", Some("Quebec"), "Canada", "America/Toronto", 1600000, "Eastern Time")

  "possibleCitiesFor" should {

    "find TZs for 'toronto'" in {
      val cities = citiesToTimezones.possibleCitiesFor("toronto")
      cities must contain(toronto)
    }

    "find TZs for 'san f'" in {
      val cities = citiesToTimezones.possibleCitiesFor("san f")
      cities must contain(sf)
    }

    "find TZs for 'montreal', which needs to find ascii name" in {
      val cities = citiesToTimezones.possibleCitiesFor("montreal")
      cities must contain(montreal)
    }

    "find TZs for 'montréal', which needs to match on accented name" in {
      val cities = citiesToTimezones.possibleCitiesFor("montréal")
      cities must contain(montreal)
    }

  }

}
