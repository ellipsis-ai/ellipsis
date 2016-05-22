package services

import java.io.{File, PrintWriter}
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.file.{Files, Paths}
import javax.inject.Inject
import com.amazonaws.services.lambda.AWSLambdaAsyncClient
import com.amazonaws.services.lambda.model._
import models.{Models, InvocationToken}
import models.bots.Behavior
import play.api.Configuration
import play.api.libs.json.{JsString, JsObject, Json}
import utils.JavaFutureWrapper
import scala.concurrent.Future
import scala.reflect.io.Path
import sys.process._

import scala.concurrent.ExecutionContext.Implicits.global

class AWSLambdaServiceImpl @Inject() (val configuration: Configuration, val models: Models) extends AWSLambdaService {

  import AWSLambdaConstants._

  val client: AWSLambdaAsyncClient = new AWSLambdaAsyncClient(credentials)
  val apiBaseUrl: String = configuration.getString(s"application.$API_BASE_URL_KEY").get

  private def resultStringFor(payload: ByteBuffer): String = {
    val bytes = payload.array
    val jsonString = new java.lang.String( bytes, Charset.forName("UTF-8") )
    val maybeResult = (Json.parse(jsonString) \ "result").toOption
    maybeResult.map(_.toString.replaceAll("^\"|\"$", "")).getOrElse {
      "Hmm. Looks like something is wrong with your script. Try `learn` again."
    }
  }

  def invoke(behavior: Behavior, params: Map[String, String]): Future[String] = {
    val token = models.runNow(InvocationToken.createFor(behavior.team))
    val payloadJson = JsObject(
      params.toSeq.map { case(k, v) => (k, JsString(v))} ++
        Seq(CONTEXT_PARAM -> JsObject(Seq(
          API_BASE_URL_KEY -> JsString(apiBaseUrl),
          TOKEN_KEY -> JsString(token.id)
        )))
    )
    val invokeRequest =
      new InvokeRequest().
        withFunctionName(behavior.functionName).
        withInvocationType(InvocationType.RequestResponse).
        withPayload(payloadJson.toString())
    JavaFutureWrapper.wrap(client.invokeAsync(invokeRequest)).map { result =>
      resultStringFor(result.getPayload)
    }
  }

  val requireRegex = """.*require\(['"]\s*(\S+)\s*['"]\).*""".r

  val alreadyIncludedModules = Array("aws-sdk", "dynamodb-doc")

  private def requiredModulesIn(code: String): Array[String] = {
    requireRegex.findAllMatchIn(code).flatMap(_.subgroups.headOption).toArray.diff(alreadyIncludedModules)
  }

  private def nodeCodeFor(functionBody: String, params: Array[String], behavior: Behavior): String = {
    val definitionParamString = (params ++ HANDLER_PARAMS ++ Array(CONTEXT_PARAM)).mkString(", ")
    val paramsFromEvent = params.indices.map(i => s"event.param$i")
    val invocationParamsString = (paramsFromEvent ++ HANDLER_PARAMS ++ Array(s"event.$CONTEXT_PARAM")).mkString(", ")
    s"""
      |exports.handler = function(event, context, callback) {
      |   var fn = function($definitionParamString) { $functionBody };
      |   var $ON_SUCCESS_PARAM = function(result) {
      |     callback(null, { "result": result === undefined ? null : result });
      |   };
      |   var $ON_ERROR_PARAM = function(err) { callback(err); };
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
      // NPM wants to write a lockfile in $HOME; this makes it work for daemons
      Process(Seq("bash","-c",s"cd $dirName && npm install $moduleName"), None, "HOME" -> "/tmp").!
    }

    Process(Seq("bash","-c",s"cd $dirName && zip -r ${zipFileNameFor(behavior.functionName)} *")).!
  }

  private def getZipFor(behavior: Behavior, functionBody: String, params: Array[String]): ByteBuffer = {
    createZipWithModulesFor(behavior, functionBody, params)
    val path = Paths.get(zipFileNameFor(behavior.functionName))
    ByteBuffer.wrap(Files.readAllBytes(path))
  }

  def deleteFunction(functionName: String): Future[Unit] = {
    val deleteFunctionRequest =
      new DeleteFunctionRequest().withFunctionName(functionName)
    try {
      JavaFutureWrapper.wrap(client.deleteFunctionAsync(deleteFunctionRequest)).map(_ => Unit)
    } catch {
      case e: ResourceNotFoundException => Future.successful(Unit)
    }
  }

  def deployFunctionFor(behavior: Behavior, functionBody: String, params: Array[String]): Future[Unit] = {
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
        withTimeout(INVOCATION_TIMEOUT_SECONDS)

    // TODO: something with the result of this
    JavaFutureWrapper.wrap(client.createFunctionAsync(createFunctionRequest)).map(_ => Unit)
  }
}
