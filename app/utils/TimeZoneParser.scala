package utils

import java.time.ZoneId

import scala.collection.JavaConversions._

case class TimeZoneIdMapping(matchString: String, id: String)

object TimeZoneParser {

  def cleanUp(tzString: String): String = tzString.replaceAll("_", " ").toLowerCase

  lazy val idMapping: Set[TimeZoneIdMapping] = {
    val ids: scala.collection.mutable.Set[String] = ZoneId.getAvailableZoneIds
    ids.map { ea =>
      TimeZoneIdMapping(cleanUp(ea), ea)
    }.toSet
  }

  def maybeZoneFor(text: String): Option[ZoneId] = {
    idMapping.
      map { ea =>
        (ea, ea.matchString.indexOf(cleanUp(text)))
      }.
      filter { case(_, index) => index >= 0 }.
      toSeq.
      sortBy { case(_, index) => index }.
      reverse.
      headOption.
      map { case(mapping, _) => ZoneId.of(mapping.id) }
  }

  def maybeZoneForId(id: String): Option[ZoneId] = {
    idMapping.find(mapping => mapping.id == id).map(mapping => ZoneId.of(mapping.id))
  }
}
