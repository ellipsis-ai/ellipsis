package services

import java.io.{File, PrintWriter}
import java.nio.ByteBuffer
import java.nio.file.{Files, Paths}
import javax.inject.Inject
import com.amazonaws.services.lambda.AWSLambdaAsyncClient
import com.amazonaws.services.lambda.model._
import models.bots.config.AWSConfig
import models.{EnvironmentVariable, Models, InvocationToken}
import models.bots._
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

  def missingEnvVarsMessageFor(envVars: Seq[String]) = {
    s"""
      |To use this behavior, you need the following environment variables defined:
      |${envVars.map( ea => s"\n- $ea").mkString("")}
      |
      |You can define an environment variable by typing something like:
      |
      |`@ellipsis: set env ENV_VAR_NAME value`
    """.stripMargin
  }

  private def contextParamDataFor(
                                   behaviorVersion: BehaviorVersion,
                                   environmentVariables: Seq[EnvironmentVariable],
                                   userInfo: UserInfo
                                   ) = {
    val token = models.runNow(InvocationToken.createFor(behaviorVersion.team))
    Seq(CONTEXT_PARAM -> JsObject(Seq(
      API_BASE_URL_KEY -> JsString(apiBaseUrl),
      TOKEN_KEY -> JsString(token.id),
      ENV_KEY -> JsObject(environmentVariables.map { ea =>
        ea.name -> JsString(ea.value)
      }),
      USER_INFO_KEY -> userInfo.toJson
    )))
  }

  def invoke(
              behaviorVersion: BehaviorVersion,
              parametersWithValues: Seq[ParameterWithValue],
              environmentVariables: Seq[EnvironmentVariable],
              event: MessageEvent
              ): Future[BehaviorResult] = {
    models.run(behaviorVersion.missingEnvironmentVariablesIn(environmentVariables)).flatMap { missingEnvVars =>
      if (missingEnvVars.nonEmpty) {
        Future.successful(MissingEnvVarsResult(missingEnvVars))
      } else if (behaviorVersion.functionBody.isEmpty) {
        Future.successful(SuccessResult(JsNull, parametersWithValues, behaviorVersion.maybeResponseTemplate, AWSLambdaLogResult.empty))
      } else {
        val token = models.runNow(InvocationToken.createFor(behaviorVersion.team))
        val userInfo = models.runNow(event.context.userInfo)
        val payloadJson = JsObject(
          parametersWithValues.map { ea => (ea.invocationName, JsString(ea.value)) } ++
            contextParamDataFor(behaviorVersion, environmentVariables, userInfo)
        )
        val invokeRequest =
          new InvokeRequest().
            withLogType(LogType.Tail).
            withFunctionName(behaviorVersion.functionName).
            withInvocationType(InvocationType.RequestResponse).
            withPayload(payloadJson.toString())
        JavaFutureWrapper.wrap(client.invokeAsync(invokeRequest)).map { result =>
          val logString = new java.lang.String(new BASE64Decoder().decodeBuffer(result.getLogResult))
          val logResult = AWSLambdaLogResult.fromText(logString, behaviorVersion.isInDevelopmentMode)
          behaviorVersion.resultFor(result.getPayload, logResult, parametersWithValues)
        }.recover {
          case e: java.util.concurrent.ExecutionException => {
            e.getMessage match {
              case amazonServiceExceptionRegex() => new AWSDownResult()
              case _ => throw e
            }
          }
        }
      }
    }
  }

  val amazonServiceExceptionRegex = """.*com\.amazonaws\.AmazonServiceException.*""".r

  val requireRegex = """.*require\(['"]\s*(\S+)\s*['"]\).*""".r

  val alreadyIncludedModules = Array("aws-sdk", "dynamodb-doc")

  private def requiredModulesIn(code: String): Array[String] = {
    requireRegex.findAllMatchIn(code).flatMap(_.subgroups.headOption).toArray.diff(alreadyIncludedModules)
  }

  private def awsCodeFor(maybeAwsConfig: Option[AWSConfig]): String = {
    maybeAwsConfig.map { awsConfig =>
      s"""
         |var AWS = require('aws-sdk');
         |var ellipsis = event.ellipsis;
         |
         |AWS.config.update({
         |  ${awsConfig.maybeAccessKeyName.map(n => s"accessKeyId: ellipsis.env.$n,").getOrElse("")}
         |  ${awsConfig.maybeSecretKeyName.map(n => s"secretAccessKey: ellipsis.env.$n,").getOrElse("")}
         |  ${awsConfig.maybeRegionName.map(n => s"region:  ellipsis.env.$n").getOrElse("")}
         | });
       """.stripMargin
    }.getOrElse("")
  }

  private def nodeCodeFor(functionBody: String, params: Array[String], behaviorVersion: BehaviorVersion, maybeAwsConfig: Option[AWSConfig]): String = {
    val paramsFromEvent = params.indices.map(i => s"event.${invocationParamFor(i)}")
    val awsParams = behaviorVersion.awsParamsFor(maybeAwsConfig)
    val invocationParamsString = (paramsFromEvent ++ HANDLER_PARAMS ++ Array(s"event.$CONTEXT_PARAM") ++ awsParams).mkString(", ")

    // Note: this attempts to make line numbers in the lambda script line up with those displayed in the UI
    // Be careful changing either this or the UI line numbers
    s"""exports.handler = function(event, context, callback) { var fn = ${behaviorVersion.functionWithParams(params, awsParams)};
      |   var $ON_SUCCESS_PARAM = function(result) {
      |     callback(null, { "result": result === undefined ? null : result });
      |   };
      |   var $ON_ERROR_PARAM = function(err) { callback(err); };
      |   ${awsCodeFor(maybeAwsConfig)}
      |   fn($invocationParamsString);
      |}
    """.stripMargin
  }

  private def dirNameFor(functionName: String) = s"/tmp/$functionName"
  private def zipFileNameFor(functionName: String) = s"${dirNameFor(functionName)}.zip"

  private def createZipWithModulesFor(behaviorVersion: BehaviorVersion, functionBody: String, params: Array[String], maybeAWSConfig: Option[AWSConfig]): Unit = {
    val dirName = dirNameFor(behaviorVersion.functionName)
    val path = Path(dirName)
    path.createDirectory()

    val writer = new PrintWriter(new File(s"$dirName/index.js"))
    writer.write(nodeCodeFor(functionBody, params, behaviorVersion, maybeAWSConfig))
    writer.close()

    requiredModulesIn(functionBody).foreach { moduleName =>
      // NPM wants to write a lockfile in $HOME; this makes it work for daemons
      Process(Seq("bash","-c",s"cd $dirName && npm install $moduleName"), None, "HOME" -> "/tmp").!
    }

    Process(Seq("bash","-c",s"cd $dirName && zip -r ${zipFileNameFor(behaviorVersion.functionName)} *")).!
  }

  private def getZipFor(behaviorVersion: BehaviorVersion, functionBody: String, params: Array[String], maybeAWSConfig: Option[AWSConfig]): ByteBuffer = {
    createZipWithModulesFor(behaviorVersion, functionBody, params, maybeAWSConfig)
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

  def deployFunctionFor(behaviorVersion: BehaviorVersion, functionBody: String, params: Array[String], maybeAWSConfig: Option[AWSConfig]): Future[Unit] = {
    val functionName = behaviorVersion.functionName

    // blocks
    deleteFunction(functionName)

    if (functionBody.trim.isEmpty) {
      Future.successful(Unit)
    } else {
      val functionCode =
        new FunctionCode().
          withZipFile(getZipFor(behaviorVersion, functionBody, params, maybeAWSConfig))
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
