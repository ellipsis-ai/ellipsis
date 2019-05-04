package controllers.api

import _root_.json.Formatting._
import _root_.json.{APIErrorData, APIResultWithErrorsData}
import controllers.api.json._
import play.api.Logger
import play.api.data.{Form, FormError}
import play.api.libs.json.{JsObject, JsValue, Json, Writes}
import play.api.mvc.{AnyContent, Request, Result}

case class APIResponder(controller: APIController) {

  import controller.request2Messages

  def logAndRespondFor(status: controller.Status, maybeErrorData: Option[APIErrorData], maybeFormErrors: Option[Seq[FormError]], details: JsValue = JsObject.empty)(implicit r: Request[_]): Result = {
    val formErrors = maybeFormErrors.map { errors =>
      errors.map { error =>
        APIErrorData(error.format, Some(error.key).filter(_.nonEmpty))
      }
    }.getOrElse(Seq())
    val errorMessage = maybeErrorData.map { data =>
      data.field.map { field =>
        s"$field: ${data.message}"
      }.getOrElse(data.message)
    }.getOrElse("")
    val errorResultData = APIResultWithErrorsData(formErrors ++ Seq(maybeErrorData).flatten)
    val jsonErrorResultData = Json.toJson(errorResultData)
    val result = status.apply(jsonErrorResultData)
    Logger.info(
      s"""Returning a ${result.header.status} for: $errorMessage
         |
         |${Json.prettyPrint(jsonErrorResultData)}
         |
         |Api info: ${Json.prettyPrint(details)}
         |
         |Request: $r with ${r.rawQueryString} ${r.body}""".stripMargin)
    result
  }

  def badRequest(maybeApiErrorData: Option[APIErrorData], maybeFormErrors: Option[Seq[FormError]], details: JsValue = JsObject.empty)(implicit r: Request[_]): Result = {
    logAndRespondFor(controller.BadRequest, maybeApiErrorData, maybeFormErrors, details)
  }

  def notFound(apiErrorData: APIErrorData, details: JsValue = JsObject.empty)(implicit r: Request[AnyContent]): Result = {
    logAndRespondFor(controller.NotFound, Some(apiErrorData), None, details)
  }

  def invalidTokenRequest[T](details: T = None)(implicit r: Request[_], tjs: Writes[T]): Result = {
    badRequest(Some(APIErrorData("Invalid token", Some("token"))), None, Json.toJson(details))
  }

  def resultForFormErrors[T <: ApiMethodInfo](formWithErrors: Form[T])(implicit r: Request[AnyContent]): Result = {
    badRequest(None, Some(formWithErrors.errors))
  }

}
