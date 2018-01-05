package json

import models.behaviors.config.requiredsimpletokenapi.RequiredSimpleTokenApi

case class RequiredSimpleTokenApiData(
                                       id: Option[String],
                                       exportId: Option[String],
                                       apiId: String,
                                       nameInCode: String
                                      ) {

  def copyForExport: RequiredSimpleTokenApiData = copy(id = None)

}

object RequiredSimpleTokenApiData {
  def from(required: RequiredSimpleTokenApi): RequiredSimpleTokenApiData = {
    RequiredSimpleTokenApiData(
      Some(required.id),
      Some(required.exportId),
      required.api.id,
      required.nameInCode
    )
  }
}
