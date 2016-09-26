package models.data.apibackeddatatype

import com.amazonaws.services.lambda.model.InvokeResult
import models.behaviors.BotResult
import models.behaviors.config.requiredoauth2apiconfig.RequiredOAuth2ApiConfig
import models.behaviors.events.MessageEvent
import models.environmentvariable.EnvironmentVariable
import models.team.Team
import org.joda.time.DateTime
import play.api.libs.json.{JsObject, JsValue}
import play.api.libs.ws.WSClient
import services.AWSLambdaService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApiBackedDataTypeNotReadyException extends Exception

case class ApiBackedDataValue(id: String, label: String)

case class ApiBackedDataTypeVersion(
                                    id: String,
                                    name: String,
                                    dataType: ApiBackedDataType,
                                    maybeHttpMethod: Option[String],
                                    maybeUrl: Option[String],
                                    maybeRequestBody: Option[JsValue],
                                    maybeFunctionBody: Option[String],
                                    createdAt: DateTime
                                    ) {

  def isReady: Boolean = maybeHttpMethod.isDefined && maybeUrl.isDefined && maybeFunctionBody.isDefined

  val functionName = id

  val jsonBody = maybeRequestBody.getOrElse(JsObject(Seq()))

  def validValuesFor(
                      maybeSearchString: Option[String],
                      event: MessageEvent,
                      ws: WSClient,
                      lambdaService: AWSLambdaService
                    ): Future[Seq[ApiBackedDataValue]] = {
    (for {
      httpMethod <- maybeHttpMethod
      url <- maybeUrl
      functionBody <- maybeFunctionBody
    } yield {
      ws.url(url).
        withMethod(httpMethod).
        withBody(jsonBody).
        execute().
        map { response =>
          response.json
          Seq[ApiBackedDataValue]()
          //lambdaService.invokeFunction(functionName, Seq(), team, event, )
        }
    }).getOrElse(Future.successful(Seq()))
  }
}
