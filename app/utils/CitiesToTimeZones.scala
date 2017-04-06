package utils

import scala.io.Source

case class CityInfo(name: String, timeZoneId: String, population: Long)

class CitiesToTimeZones {

  val trie: Trie = Trie()
  val nameToTzMap = scala.collection.mutable.Map[String, Set[CityInfo]]()

  val dataStream = getClass.getResourceAsStream("data/cities15000.txt")
  val cityNameColumnIndex = 1
  val timeZoneColumnIndex = 17
  val populationColumnIndex = 14

  def load(): Unit = {
    val bufferedSource = Source.fromInputStream(dataStream)
    for (line <- bufferedSource.getLines) {
      val cols = line.split("\t").map(_.trim)
      val name = cols(cityNameColumnIndex)
      val tz = cols(timeZoneColumnIndex)
      val population: Long = try {
        cols(populationColumnIndex).toLong
      } catch {
        case e: NumberFormatException => 15000 // membership in the list means at least this many people
      }
      val info = CityInfo(name, tz, population)

      trie.append(name)

      val existingValues = nameToTzMap.getOrElseUpdate(name, Set())
      nameToTzMap.put(name, (existingValues ++ Set(info)))
    }
    bufferedSource.close
  }

  load()

  def possibleCitiesFor(searchQuery: String): Seq[CityInfo] = {
    val names = trie.findByPrefix(searchQuery)
    names.flatMap { ea =>
      nameToTzMap.getOrElse(ea, Set())
    }.distinct.sortBy(_.population).reverse
  }

}
