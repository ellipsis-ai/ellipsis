package services

import java.nio.charset.Charset
import javax.inject.Inject
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.lambda.AWSLambdaClient
import com.amazonaws.services.lambda.model.{InvocationType, InvokeRequest}
import play.api.Configuration
import play.api.libs.json.Json


class AWSLambdaServiceImpl @Inject() (val configuration: Configuration) extends AWSLambdaService {

  val credentials = new BasicAWSCredentials(configuration.getString("aws.accessKey").getOrElse("foo"), configuration.getString("aws.secretKey").getOrElse("foo"))

  val blockingClient: AWSLambdaClient = new AWSLambdaClient(credentials)

  def invoke(functionName: String, params: Map[String, String]): String = {
    val invokeRequest =
      new InvokeRequest().
        withFunctionName(functionName).
        withInvocationType(InvocationType.RequestResponse).
        withPayload(Json.toJson(params).toString())
    val result = blockingClient.invoke(invokeRequest)
    val bytes = result.getPayload.array
    val jsonString = new java.lang.String( bytes, Charset.forName("UTF-8") )
    (Json.parse(jsonString) \ "result").get.as[String]
  }
}
