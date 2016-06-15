package services

import java.io.{File, PrintWriter}
import java.nio.ByteBuffer
import java.nio.file.{Files, Paths}
import javax.inject.Inject
import com.amazonaws.services.lambda.AWSLambdaAsyncClient
import com.amazonaws.services.lambda.model._
import models.{EnvironmentVariable, Models, InvocationToken}
import models.bots.{ParameterWithValue, BehaviorVersion}
import play.api.Configuration
import play.api.libs.json._
import sun.misc.BASE64Decoder
import utils.JavaFutureWrapper
import scala.concurrent.Future
import scala.reflect.io.Path
import sys.process._

import scala.concurrent.ExecutionContext.Implicits.global

class AWSLambdaServiceImpl @Inject() (val configuration: Configuration, val models: Models) extends AWSLambdaService {

  import AWSLambdaConstants._

  val client: AWSLambdaAsyncClient = new AWSLambdaAsyncClient(credentials)
  val apiBaseUrl: String = configuration.getString(s"application.$API_BASE_URL_KEY").get

  def invoke(behaviorVersion: BehaviorVersion, parametersWithValues: Seq[ParameterWithValue], environmentVariables: Seq[EnvironmentVariable]): Future[String] = {
    if (behaviorVersion.functionBody.isEmpty) {
      Future.successful(behaviorVersion.successResultStringFor(JsNull, parametersWithValues))
    } else {
      val token = models.runNow(InvocationToken.createFor(behaviorVersion.team))
      val payloadJson = JsObject(
        parametersWithValues.map { ea => (ea.invocationName, JsString(ea.value)) } ++
          Seq(CONTEXT_PARAM -> JsObject(Seq(
            API_BASE_URL_KEY -> JsString(apiBaseUrl),
            TOKEN_KEY -> JsString(token.id),
            ENV_KEY -> JsObject(environmentVariables.map { ea =>
              ea.name -> JsString(ea.value)
            })
          )))
      )
      val invokeRequest =
        new InvokeRequest().
          withLogType(LogType.Tail).
          withFunctionName(behaviorVersion.functionName).
          withInvocationType(InvocationType.RequestResponse).
          withPayload(payloadJson.toString())
      JavaFutureWrapper.wrap(client.invokeAsync(invokeRequest)).map { result =>
        val logResult = new java.lang.String(new BASE64Decoder().decodeBuffer(result.getLogResult))
        behaviorVersion.resultStringFor(result.getPayload, logResult, parametersWithValues)
      }
    }
  }

  val requireRegex = """.*require\(['"]\s*(\S+)\s*['"]\).*""".r

  val alreadyIncludedModules = Array("aws-sdk", "dynamodb-doc")

  private def requiredModulesIn(code: String): Array[String] = {
    requireRegex.findAllMatchIn(code).flatMap(_.subgroups.headOption).toArray.diff(alreadyIncludedModules)
  }

  private def nodeCodeFor(functionBody: String, params: Array[String], behaviorVersion: BehaviorVersion): String = {
    val paramsFromEvent = params.indices.map(i => s"event.${invocationParamFor(i)}")
    val invocationParamsString = (paramsFromEvent ++ HANDLER_PARAMS ++ Array(s"event.$CONTEXT_PARAM")).mkString(", ")

    val definitionUserParamsString = if (params.isEmpty) {
      ""
    } else {
      s"""\n${params.map(ea => ea ++ ",").mkString("\n")}\n"""
    }
    val definitionBuiltinParamsString = (HANDLER_PARAMS ++ Array(CONTEXT_PARAM)).mkString(", ")
    // Note: this attempts to make line numbers in the lambda script line up with those displayed in the UI
    // Be careful changing either this or the UI line numbers
    s"""exports.handler = function(event, context, callback) { var fn = function($definitionUserParamsString$definitionBuiltinParamsString) {
      |     $functionBody
      |   };
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

  private def createZipWithModulesFor(behaviorVersion: BehaviorVersion, functionBody: String, params: Array[String]): Unit = {
    val dirName = dirNameFor(behaviorVersion.functionName)
    val path = Path(dirName)
    path.createDirectory()

    val writer = new PrintWriter(new File(s"$dirName/index.js"))
    writer.write(nodeCodeFor(functionBody, params, behaviorVersion))
    writer.close()

    requiredModulesIn(functionBody).foreach { moduleName =>
      // NPM wants to write a lockfile in $HOME; this makes it work for daemons
      Process(Seq("bash","-c",s"cd $dirName && npm install $moduleName"), None, "HOME" -> "/tmp").!
    }

    Process(Seq("bash","-c",s"cd $dirName && zip -r ${zipFileNameFor(behaviorVersion.functionName)} *")).!
  }

  private def getZipFor(behaviorVersion: BehaviorVersion, functionBody: String, params: Array[String]): ByteBuffer = {
    createZipWithModulesFor(behaviorVersion, functionBody, params)
    val path = Paths.get(zipFileNameFor(behaviorVersion.functionName))
    ByteBuffer.wrap(Files.readAllBytes(path))
  }

  def deleteFunction(functionName: String): Unit = {
    val deleteFunctionRequest =
      new DeleteFunctionRequest().withFunctionName(functionName)
    try {
      client.deleteFunction(deleteFunctionRequest)
    } catch {
      case e: ResourceNotFoundException => Unit // we expect this when creating the first time
    }
  }

  def deployFunctionFor(behaviorVersion: BehaviorVersion, functionBody: String, params: Array[String]): Future[Unit] = {
    val functionName = behaviorVersion.functionName

    // blocks
    deleteFunction(functionName)

    if (functionBody.trim.isEmpty) {
      Future.successful(Unit)
    } else {
      val functionCode =
        new FunctionCode().
          withZipFile(getZipFor(behaviorVersion, functionBody, params))
      val createFunctionRequest =
        new CreateFunctionRequest().
          withFunctionName(functionName).
          withCode(functionCode).
          withRole(configuration.getString("aws.role").get).
          withRuntime(com.amazonaws.services.lambda.model.Runtime.Nodejs43).
          withHandler("index.handler").
          withTimeout(INVOCATION_TIMEOUT_SECONDS)

      JavaFutureWrapper.wrap(client.createFunctionAsync(createFunctionRequest)).map(_ => Unit)
    }
  }
}
