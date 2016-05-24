package services

import java.io.{File, PrintWriter}
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.file.{Files, Paths}
import javax.inject.Inject
import com.amazonaws.services.lambda.AWSLambdaAsyncClient
import com.amazonaws.services.lambda.model._
import models.{EnvironmentVariable, Models, InvocationToken}
import models.bots.Behavior
import play.api.Configuration
import play.api.libs.json.{JsValue, JsString, JsObject, Json}
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

  private def processedResultFor(result: JsValue): String = {
    result.
      toString.
      replaceAll("^\"|\"$", "")
  }

  private def successResultStringFor(result: JsValue): String = {
    processedResultFor(result)
  }

  private def errorResultStringFor(json: JsValue, logResult: String): String = {
    val maybeMessage = (json \ "errorMessage").toOption.flatMap { m =>
      if ("Process exited before completing request".r.findFirstIn(m.toString).isDefined) {
        None
      } else {
        Some(m)
      }
    }
    val logRegex = """.*\n.*\t.*\t(.*)""".r
    val maybeLogError = logRegex.findFirstMatchIn(logResult).flatMap(_.subgroups.headOption)
    Array(maybeMessage, maybeLogError).flatten.mkString(": ")
  }

  private def handledErrorResultStringFor(json: JsValue): String = {
    val prompt = s"$ON_ERROR_PARAM triggered"
    val maybeDetail = (json \ "errorMessage").toOption.map(processedResultFor)
    Array(Some(prompt), maybeDetail).flatten.mkString(": ")
  }

  private def unhandledErrorResultStringFor(logResult: String): String = {
    val prompt = s"We hit an error before calling $ON_SUCCESS_PARAM or $ON_ERROR_PARAM"
    val logRegex = """.*\n.*\t.*\t(.*)""".r
    val maybeDetail = logRegex.findFirstMatchIn(logResult).flatMap(_.subgroups.headOption)
    Array(Some(prompt), maybeDetail).flatten.mkString(": ")
  }

  private def isUnhandledError(json: JsValue): Boolean = {
    (json \ "errorMessage").toOption.flatMap { m =>
      "Process exited before completing request".r.findFirstIn(m.toString)
    }.isDefined
  }

  private def resultStringFor(payload: ByteBuffer, logResult: String): String = {
    val bytes = payload.array
    val jsonString = new java.lang.String( bytes, Charset.forName("UTF-8") )
    val json = Json.parse(jsonString)
    (json \ "result").toOption.map { successResult =>
      successResultStringFor(successResult)
    }.getOrElse {
      if (isUnhandledError(json)) {
        unhandledErrorResultStringFor(logResult)
      } else {
        handledErrorResultStringFor(json)
      }
    }
  }

  def invoke(behavior: Behavior, params: Map[String, String], environmentVariables: Seq[EnvironmentVariable]): Future[String] = {
    val token = models.runNow(InvocationToken.createFor(behavior.team))
    val payloadJson = JsObject(
      params.toSeq.map { case(k, v) => (k, JsString(v))} ++
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
        withFunctionName(behavior.functionName).
        withInvocationType(InvocationType.RequestResponse).
        withPayload(payloadJson.toString())
    JavaFutureWrapper.wrap(client.invokeAsync(invokeRequest)).map { result =>
      val logResult = new java.lang.String(new BASE64Decoder().decodeBuffer(result.getLogResult))
      resultStringFor(result.getPayload, logResult)
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

  def deleteFunction(functionName: String): Unit = {
    val deleteFunctionRequest =
      new DeleteFunctionRequest().withFunctionName(functionName)
    try {
      client.deleteFunction(deleteFunctionRequest)
    } catch {
      case e: ResourceNotFoundException => Unit // we expect this when creating the first time
    }
  }

  def deployFunctionFor(behavior: Behavior, functionBody: String, params: Array[String]): Future[Unit] = {
    val functionName = behavior.functionName

    // blocks
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

    JavaFutureWrapper.wrap(client.createFunctionAsync(createFunctionRequest)).map(_ => Unit)
  }
}
