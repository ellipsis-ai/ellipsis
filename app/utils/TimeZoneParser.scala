package utils

import org.joda.time.DateTimeZone
import scala.collection.JavaConversions._

case class TimeZoneIdMapping(matchString: String, id: String)

object TimeZoneParser {

  def cleanUp(tzString: String): String = tzString.replaceAll("_", " ").toLowerCase

  lazy val idMapping: Set[TimeZoneIdMapping] = {
    val ids: scala.collection.mutable.Set[String] = DateTimeZone.getAvailableIDs
    ids.map { ea =>
      TimeZoneIdMapping(cleanUp(ea), ea)
    }.toSet
  }

  def maybeZoneFor(text: String): Option[DateTimeZone] = {
    idMapping.
      map { ea =>
        (ea, ea.matchString.indexOf(cleanUp(text)))
      }.
      filter { case(_, index) => index >= 0 }.
      toSeq.
      sortBy { case(_, index) => index }.
      reverse.
      headOption.
      map { case(mapping, _) => DateTimeZone.forID(mapping.id) }
  }

}
