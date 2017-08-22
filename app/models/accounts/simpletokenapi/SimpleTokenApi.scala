package models.accounts.simpletokenapi

import utils.NameFormatter

case class SimpleTokenApi(
                          id: String,
                          name: String,
                          maybeTokenUrl: Option[String],
                          maybeTeamId: Option[String]
                        ) {

  def keyName: String = NameFormatter.formatConfigPropertyName(name)

}
