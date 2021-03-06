package utils

import scala.io.Source

case class CityInfo(name: String, asciiName: String, admin: Option[String], country: String, timeZoneId: String, population: Long, timeZoneName: String)

class CitiesToTimeZones {

  val trie: Trie = Trie()
  val infoMap = scala.collection.mutable.Map[String, Set[CityInfo]]()
  val dataPath = "/data"

  def loadAdmins: Map[String, String] = {
    val adminCodeColumnIndex = 0
    val adminNameColumnIndex = 1
    val dataStream = getClass.getResourceAsStream(s"$dataPath/admin1CodesASCII.txt")
    val bufferedSource = Source.fromInputStream(dataStream)
    val map = scala.collection.mutable.Map[String, String]()
    for (line <- bufferedSource.getLines) {
      val cols = line.split("\t").map(_.trim)
      val code = cols(adminCodeColumnIndex)
      val name = cols(adminNameColumnIndex)
      map.put(code, name)
    }
    bufferedSource.close
    map.toMap
  }

  def loadCountries: Map[String, String] = {
    val countryCodeColumnIndex = 0
    val countryNameColumnIndex = 4
    val dataStream = getClass.getResourceAsStream(s"$dataPath/countryInfo.txt")
    val bufferedSource = Source.fromInputStream(dataStream)
    val map = scala.collection.mutable.Map[String, String]()
    for (line <- bufferedSource.getLines) {
      val cols = line.split("\t").map(_.trim)
      val code = cols(countryCodeColumnIndex)
      val name = cols(countryNameColumnIndex)
      map.put(code, name)
    }
    bufferedSource.close
    map.toMap
  }

  def addToInfoMap(key: String, info: CityInfo) = {
    trie.append(key)
    val existingValues = infoMap.getOrElseUpdate(key, Set())
    infoMap.put(key, (existingValues ++ Set(info)))
  }

  def load(): Unit = {
    val cityNameColumnIndex = 1
    val asciiNameColumnIndex = 2
    val countryCodeColumnIndex = 8
    val admin1CodeColumnIndex = 10
    val timeZoneColumnIndex = 17
    val populationColumnIndex = 14

    val admins = loadAdmins
    val countries = loadCountries

    val dataStream = getClass.getResourceAsStream(s"$dataPath/cities15000.txt")
    val bufferedSource = Source.fromInputStream(dataStream)
    for (line <- bufferedSource.getLines) {
      val cols = line.split("\t").map(_.trim)

      val name = cols(cityNameColumnIndex)
      val asciiName = cols(asciiNameColumnIndex)
      val countryCode = cols(countryCodeColumnIndex)
      val country = countries(countryCode)
      val admin1Code = cols(admin1CodeColumnIndex)
      val admin1 = admins.get(s"$countryCode.$admin1Code")
      val tz = cols(timeZoneColumnIndex)
      val population: Long = try {
        cols(populationColumnIndex).toLong
      } catch {
        case e: NumberFormatException => 15000 // membership in the list means at least this many people
      }

      TimeZoneParser.maybeNameForZoneId(tz).map { timeZoneName =>
        val info = CityInfo(name, asciiName, admin1, country, tz, population, timeZoneName)

        addToInfoMap(name, info)
        addToInfoMap(asciiName, info)
        addToInfoMap(tz, info)
      }
    }
    bufferedSource.close
  }

  load()

  def distinctByPlaceAndTzId(cities: Seq[CityInfo]): Seq[CityInfo] = {
    cities.groupBy { ea =>
      List[String](ea.name, ea.admin.getOrElse(""), ea.country, ea.timeZoneId).filter(_.nonEmpty).mkString("|")
    }.map(_._2.head).toSeq
  }

  def possibleCitiesFor(searchQuery: String): Seq[CityInfo] = {
    val names = trie.findByPrefix(searchQuery)
    val matchingCities = names.flatMap { ea =>
      infoMap.getOrElse(ea, Set())
    }
    distinctByPlaceAndTzId(matchingCities).sortBy(_.population).reverse.slice(0, 100)
  }

}
