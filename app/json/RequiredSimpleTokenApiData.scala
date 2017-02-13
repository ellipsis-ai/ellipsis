package json

import models.behaviors.config.requiredsimpletokenapi.RequiredSimpleTokenApi

case class RequiredSimpleTokenApiData(
                                        id: Option[String],
                                        apiId: String,
                                        name: String
                                      ) {

  def copyForExport: RequiredSimpleTokenApiData = copy(id = None)

}

object RequiredSimpleTokenApiData {
  def from(required: RequiredSimpleTokenApi): RequiredSimpleTokenApiData = {
    RequiredSimpleTokenApiData(
      Some(required.id),
      required.api.id,
      required.api.name
    )
  }
}
