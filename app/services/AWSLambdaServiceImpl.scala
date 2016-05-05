package services

import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.file.{Files, Paths}
import java.util.zip.{ZipEntry, ZipOutputStream}
import javax.inject.Inject
import com.amazonaws.AmazonServiceException
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.lambda.AWSLambdaClient
import com.amazonaws.services.lambda.model._
import play.api.Configuration
import play.api.libs.json.Json


class AWSLambdaServiceImpl @Inject() (val configuration: Configuration) extends AWSLambdaService {

  val credentials = new BasicAWSCredentials(configuration.getString("aws.accessKey").getOrElse("foo"), configuration.getString("aws.secretKey").getOrElse("foo"))

  val blockingClient: AWSLambdaClient = new AWSLambdaClient(credentials)

  private def resultStringFor(payload: ByteBuffer): String = {
    val bytes = payload.array
    val jsonString = new java.lang.String( bytes, Charset.forName("UTF-8") )
    val maybeResult = (Json.parse(jsonString) \ "result").toOption
    maybeResult.map(_.toString.replaceAll("^\"|\"$", "")).getOrElse {
      "Hmm. Looks like something is wrong with your script. Try `learn` again."
    }
  }

  def invoke(functionName: String, params: Map[String, String]): String = {
    var invokeRequest =
      new InvokeRequest().
        withFunctionName(functionName).
        withInvocationType(InvocationType.RequestResponse)
    if (params.nonEmpty) {
      invokeRequest = invokeRequest.withPayload(Json.toJson(params).toString())
    }
    val result = blockingClient.invoke(invokeRequest)
    resultStringFor(result.getPayload)
  }

  private def nodeCodeFor(code: String, params: Array[String]): String = {
    var fixedCode = "[“”]".r.replaceAllIn(code, "\"")
    "‘".r.replaceAllIn(fixedCode, "'")
    val paramString = params.indices.map(i => s"event.param$i").mkString(", ")
    s"""
      |exports.handler = function(event, context, callback) {
      |   var fn = $fixedCode;
      |   callback(null, { "result": fn($paramString) });
      |}
    """.stripMargin
  }

  private def zipFileNameFor(functionName: String) = s"/tmp/$functionName.zip"

  private def createZipFor(functionName: String, code: String, params: Array[String]): ZipOutputStream = {
    val fileStream = new FileOutputStream(zipFileNameFor(functionName))
    val zipStream = new ZipOutputStream(fileStream)

    val zipEntry = new ZipEntry(s"$functionName.js")
    zipStream.putNextEntry(zipEntry)
    val data = nodeCodeFor(code, params).getBytes
    zipStream.write(data, 0, data.length)
    zipStream.closeEntry()

    zipStream.close
    zipStream
  }

  private def getZipFor(functionName: String, code: String, params: Array[String]): ByteBuffer = {
    createZipFor(functionName, code, params)
    val path = Paths.get(zipFileNameFor(functionName))
    ByteBuffer.wrap(Files.readAllBytes(path))
  }

  def deleteFunction(functionName: String): Unit = {
    val deleteFunctionRequest =
      new DeleteFunctionRequest().withFunctionName(functionName)
    try {
      blockingClient.deleteFunction(deleteFunctionRequest)
    } catch {
      case e: ResourceNotFoundException => Unit
    }
  }

  def deployFunction(functionName: String, code: String, params: Array[String]): Unit = {
    deleteFunction(functionName)
    val functionCode =
      new FunctionCode().
        withZipFile(getZipFor(functionName, code, params))
    val createFunctionRequest =
      new CreateFunctionRequest().
        withFunctionName(functionName).
        withCode(functionCode).
        withRole(configuration.getString("aws.role").get).
        withRuntime(com.amazonaws.services.lambda.model.Runtime.Nodejs43).
        withHandler(s"$functionName.handler").
        withTimeout(10)

    blockingClient.createFunction(createFunctionRequest)
  }
}
