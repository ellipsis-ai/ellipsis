package models.accounts.simpletokenapi

import org.apache.commons.lang.WordUtils

case class SimpleTokenApi(
                          id: String,
                          name: String,
                          maybeTokenUrl: Option[String],
                          maybeTeamId: Option[String]
                        ) {

  def keyName: String = {
    val capitalized = WordUtils.capitalize(name).replaceAll("\\s", "")
    capitalized.substring(0, 1).toLowerCase() + capitalized.substring(1)
  }

}
