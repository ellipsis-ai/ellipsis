package utils

import scala.io.Source

case class CityInfo(name: String, asciiName: String, timeZoneId: String, population: Long)

class CitiesToTimeZones {

  val trie: Trie = Trie()
  val infoMap = scala.collection.mutable.Map[String, Set[CityInfo]]()

  val dataStream = getClass.getResourceAsStream("data/cities15000.txt")
  val cityNameColumnIndex = 1
  val asciiNameColumnIndex = 2
  val timeZoneColumnIndex = 17
  val populationColumnIndex = 14

  def addToInfoMap(key: String, info: CityInfo) = {
    trie.append(key)
    val existingValues = infoMap.getOrElseUpdate(key, Set())
    infoMap.put(key, (existingValues ++ Set(info)))
  }

  def load(): Unit = {
    val bufferedSource = Source.fromInputStream(dataStream)
    for (line <- bufferedSource.getLines) {
      val cols = line.split("\t").map(_.trim)
      val name = cols(cityNameColumnIndex)
      val asciiName = cols(asciiNameColumnIndex)
      val tz = cols(timeZoneColumnIndex)
      val population: Long = try {
        cols(populationColumnIndex).toLong
      } catch {
        case e: NumberFormatException => 15000 // membership in the list means at least this many people
      }
      val info = CityInfo(name, asciiName, tz, population)

      addToInfoMap(name, info)
      addToInfoMap(asciiName, info)
    }
    bufferedSource.close
  }

  load()

  def possibleCitiesFor(searchQuery: String): Seq[CityInfo] = {
    val names = trie.findByPrefix(searchQuery)
    names.flatMap { ea =>
      infoMap.getOrElse(ea, Set())
    }.distinct.sortBy(_.population).reverse
  }

}
