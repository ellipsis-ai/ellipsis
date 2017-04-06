package utils

import scala.io.Source

class CitiesToTimeZones {

  val trie: Trie = Trie()
  val nameToTzMap: scala.collection.mutable.Map[String, Set[String]] = scala.collection.mutable.Map[String, Set[String]]()

  val dataStream = getClass.getResourceAsStream("data/cities15000.txt")
  val cityNameColumnIndex = 1
  val timeZoneColumnIndex = 17

  def load(): Unit = {
    val bufferedSource = Source.fromInputStream(dataStream)
    for (line <- bufferedSource.getLines) {
      val cols = line.split("\t").map(_.trim)
      val name = cols(cityNameColumnIndex)
      val tz = cols(timeZoneColumnIndex)
      trie.append(name)
      val existingValues = nameToTzMap.getOrElseUpdate(name, Set())
      nameToTzMap.put(name, (existingValues ++ Set(tz)))
    }
    bufferedSource.close
  }

  load()

  def possibleTimeZonesFor(searchQuery: String): Seq[String] = {
    val names = trie.findByPrefix(searchQuery)
    names.flatMap { ea =>
      nameToTzMap.getOrElse(ea, Set())
    }.distinct
  }

}
