package services

import java.io.{File, PrintWriter}
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.file.{Files, Paths}
import javax.inject.Inject
import com.amazonaws.services.lambda.AWSLambdaClient
import com.amazonaws.services.lambda.model._
import models.{Models, InvocationToken}
import models.bots.Behavior
import play.api.Configuration
import play.api.libs.json.{JsString, JsObject, Json}
import scala.reflect.io.{Path}
import sys.process._


class AWSLambdaServiceImpl @Inject() (val configuration: Configuration, val models: Models) extends AWSLambdaService {

  val blockingClient: AWSLambdaClient = new AWSLambdaClient(credentials)

  private def resultStringFor(payload: ByteBuffer): String = {
    val bytes = payload.array
    val jsonString = new java.lang.String( bytes, Charset.forName("UTF-8") )
    val maybeResult = (Json.parse(jsonString) \ "result").toOption
    maybeResult.map(_.toString.replaceAll("^\"|\"$", "")).getOrElse {
      "Hmm. Looks like something is wrong with your script. Try `learn` again."
    }
  }

  val CONTEXT = "context"
  val TOKEN = "token"
  val API_BASE_URL = "apiBaseUrl"
  def apiBaseUrl: String = configuration.getString("application.apiBaseUrl").get

  def invoke(behavior: Behavior, params: Map[String, String]): String = {
    val token = models.runNow(InvocationToken.createFor(behavior.team))
    val payloadJson = JsObject(
      params.toSeq.map { case(k, v) => (k, JsString(v))} ++
        Seq(CONTEXT -> JsObject(Seq(
          API_BASE_URL -> JsString(apiBaseUrl),
          TOKEN -> JsString(token.id)
        )))
    )
    val invokeRequest =
      new InvokeRequest().
        withFunctionName(behavior.functionName).
        withInvocationType(InvocationType.RequestResponse).
        withPayload(payloadJson.toString())
    val result = blockingClient.invoke(invokeRequest)
    resultStringFor(result.getPayload)
  }

  val requireRegex = """.*require\(['"]\s*(\S+)\s*['"]\).*""".r

  val alreadyIncludedModules = Array("aws-sdk", "dynamodb-doc")

  private def requiredModulesIn(code: String): Array[String] = {
    requireRegex.findAllMatchIn(code).flatMap(_.subgroups.headOption).toArray.diff(alreadyIncludedModules)
  }

  val builtInParams = Array("onSuccess", "onError", "context")

  private def nodeCodeFor(functionBody: String, params: Array[String], behavior: Behavior): String = {
    val definitionParamString = (params ++ builtInParams).mkString(", ")
    val paramsFromEvent = params.indices.map(i => s"event.param$i")
    val invocationParamsString = (paramsFromEvent ++ builtInParams).mkString(", ")
    s"""
      |exports.handler = function(event, context, callback) {
      |   var context = event.$CONTEXT;
      |   var fn = function($definitionParamString) { $functionBody };
      |   var onSuccess = function(result) { callback(null, { "result": result }); };
      |   var onError = function(err) { callback(err); };
      |   fn($invocationParamsString);
      |}
    """.stripMargin
  }

  private def dirNameFor(functionName: String) = s"/tmp/$functionName"
  private def zipFileNameFor(functionName: String) = s"${dirNameFor(functionName)}.zip"

  private def createZipWithModulesFor(behavior: Behavior, functionBody: String, params: Array[String]): Unit = {
    val dirName = dirNameFor(behavior.functionName)
    val path = Path(dirName)
    path.createDirectory()

    val writer = new PrintWriter(new File(s"$dirName/index.js"))
    writer.write(nodeCodeFor(functionBody, params, behavior))
    writer.close()

    requiredModulesIn(functionBody).foreach { moduleName =>
      Process(Seq("bash","-c",s"HOME=/tmp cd $dirName && npm install $moduleName")).!
    }

    Process(Seq("bash","-c",s"cd $dirName && zip -r ${zipFileNameFor(behavior.functionName)} *")).!
  }

  private def getZipFor(behavior: Behavior, functionBody: String, params: Array[String]): ByteBuffer = {
    createZipWithModulesFor(behavior, functionBody, params)
    val path = Paths.get(zipFileNameFor(behavior.functionName))
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

  def deployFunctionFor(behavior: Behavior, functionBody: String, params: Array[String]): Unit = {
    val functionName = behavior.functionName
    deleteFunction(functionName)
    val functionCode =
      new FunctionCode().
        withZipFile(getZipFor(behavior, functionBody, params))
    val createFunctionRequest =
      new CreateFunctionRequest().
        withFunctionName(functionName).
        withCode(functionCode).
        withRole(configuration.getString("aws.role").get).
        withRuntime(com.amazonaws.services.lambda.model.Runtime.Nodejs43).
        withHandler("index.handler").
        withTimeout(10)

    blockingClient.createFunction(createFunctionRequest)
  }
}
