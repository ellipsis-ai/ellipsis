package services

import java.io.{File, PrintWriter}
import java.nio.ByteBuffer
import java.nio.file.{Files, Paths}
import javax.inject.Inject
import com.amazonaws.services.lambda.AWSLambdaAsyncClient
import com.amazonaws.services.lambda.model._
import models.bots.config.{RequiredOAuth2ApplicationQueries, RequiredOAuth2Application, AWSConfig}
import models.{EnvironmentVariable, Models, InvocationToken}
import models.bots._
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.libs.json._
import play.api.libs.ws.WSClient
import sun.misc.BASE64Decoder
import utils.JavaFutureWrapper
import scala.concurrent.Future
import scala.reflect.io.Path
import sys.process._

import scala.concurrent.ExecutionContext.Implicits.global

class AWSLambdaServiceImpl @Inject() (
                                       val configuration: Configuration,
                                       val models: Models,
                                       val ws: WSClient,
                                       val cache: CacheApi
                                       ) extends AWSLambdaService {

  import AWSLambdaConstants._

  val client: AWSLambdaAsyncClient = new AWSLambdaAsyncClient(credentials)
  val apiBaseUrl: String = configuration.getString(s"application.$API_BASE_URL_KEY").get

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
    for {
      missingEnvVars <- models.run(behaviorVersion.missingEnvironmentVariablesIn(environmentVariables))
      requiredOAuth2Applications <- models.run(RequiredOAuth2ApplicationQueries.allFor(behaviorVersion))
      result <- if (missingEnvVars.nonEmpty) {
        Future.successful(MissingEnvVarsResult(missingEnvVars))
      } else if (behaviorVersion.functionBody.isEmpty) {
        Future.successful(SuccessResult(JsNull, parametersWithValues, behaviorVersion.maybeResponseTemplate, AWSLambdaLogResult.empty))
      } else {
        for {
          token <- models.run(InvocationToken.createFor(behaviorVersion.team))
          userInfo <- models.run(event.context.userInfo(ws))
          missingOAuth2Applications <- Future.successful(requiredOAuth2Applications.filter { ea =>
            !userInfo.links.exists(_.externalSystem == ea.application.name)
          })
          result <- missingOAuth2Applications.headOption.map { firstMissingOAuth2App =>
            Future.successful(OAuth2TokenMissing(firstMissingOAuth2App.application, event, cache, configuration))
          }.getOrElse {
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
        } yield result
      }
    } yield result
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
         |
         |AWS.config.update({
         |  ${awsConfig.maybeAccessKeyName.map(n => s"accessKeyId: $CONTEXT_PARAM.env.$n,").getOrElse("")}
         |  ${awsConfig.maybeSecretKeyName.map(n => s"secretAccessKey: $CONTEXT_PARAM.env.$n,").getOrElse("")}
         |  ${awsConfig.maybeRegionName.map(n => s"region: $CONTEXT_PARAM.env.$n").getOrElse("")}
         | });
       """.stripMargin
    }.getOrElse("")
  }

  private def accessTokenCodeFor(app: RequiredOAuth2Application): String = {
    s"""var ${app.application.parameterName} = event.$CONTEXT_PARAM.userInfo.links.find((ea) => ea.externalSystem == "${app.application.name}").oauthToken;"""
  }

  private def accessTokensCodeFor(requiredOAuth2Applications: Seq[RequiredOAuth2Application]): String = {
    requiredOAuth2Applications.map(accessTokenCodeFor).mkString("\n")
  }

  private def nodeCodeFor(functionBody: String, params: Array[String], behaviorVersion: BehaviorVersion, maybeAwsConfig: Option[AWSConfig], requiredOAuth2Applications: Seq[RequiredOAuth2Application]): String = {
    val paramsFromEvent = params.indices.map(i => s"event.${invocationParamFor(i)}")
    val awsParams = behaviorVersion.awsParamsFor(maybeAwsConfig)
    val accessTokenParams = behaviorVersion.accessTokenParamsFor(requiredOAuth2Applications)
    val invocationParamsString = (paramsFromEvent ++ HANDLER_PARAMS ++ Array(s"event.$CONTEXT_PARAM") ++ awsParams ++ accessTokenParams).mkString(", ")

    // Note: this attempts to make line numbers in the lambda script line up with those displayed in the UI
    // Be careful changing either this or the UI line numbers
    s"""exports.handler = function(event, context, callback) { var fn = ${behaviorVersion.functionWithParams(params, awsParams, accessTokenParams)};
      |   var $CONTEXT_PARAM = event.$CONTEXT_PARAM;
      |   $CONTEXT_PARAM.noResponse = function() {
      |     callback(null, { $NO_RESPONSE_KEY: true });
      |   };
      |   var $ON_SUCCESS_PARAM = function(result) {
      |     callback(null, { "result": result === undefined ? null : result });
      |   };
      |   var $ON_ERROR_PARAM = function(err) { callback(err); };
      |   ${awsCodeFor(maybeAwsConfig)}
      |   ${accessTokensCodeFor(requiredOAuth2Applications)}
      |   fn($invocationParamsString);
      |}
    """.stripMargin
  }

  private def dirNameFor(functionName: String) = s"/tmp/$functionName"
  private def zipFileNameFor(functionName: String) = s"${dirNameFor(functionName)}.zip"

  private def createZipWithModulesFor(
                                       behaviorVersion: BehaviorVersion,
                                       functionBody: String,
                                       params: Array[String],
                                       maybeAWSConfig: Option[AWSConfig],
                                       requiredOAuth2Applications: Seq[RequiredOAuth2Application]
                                       ): Unit = {
    val dirName = dirNameFor(behaviorVersion.functionName)
    val path = Path(dirName)
    path.createDirectory()

    val writer = new PrintWriter(new File(s"$dirName/index.js"))
    writer.write(nodeCodeFor(functionBody, params, behaviorVersion, maybeAWSConfig, requiredOAuth2Applications))
    writer.close()

    requiredModulesIn(functionBody).foreach { moduleName =>
      // NPM wants to write a lockfile in $HOME; this makes it work for daemons
      Process(Seq("bash","-c",s"cd $dirName && npm install $moduleName"), None, "HOME" -> "/tmp").!
    }

    Process(Seq("bash","-c",s"cd $dirName && zip -r ${zipFileNameFor(behaviorVersion.functionName)} *")).!
  }

  private def getZipFor(
                         behaviorVersion: BehaviorVersion,
                         functionBody: String,
                         params: Array[String],
                         maybeAWSConfig: Option[AWSConfig],
                         requiredOAuth2Applications: Seq[RequiredOAuth2Application]
                         ): ByteBuffer = {
    createZipWithModulesFor(behaviorVersion, functionBody, params, maybeAWSConfig, requiredOAuth2Applications)
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

  def deployFunctionFor(
                         behaviorVersion: BehaviorVersion,
                         functionBody: String,
                         params: Array[String],
                         maybeAWSConfig: Option[AWSConfig],
                         requiredOAuth2Applications: Seq[RequiredOAuth2Application]
                         ): Future[Unit] = {
    val functionName = behaviorVersion.functionName

    // blocks
    deleteFunction(functionName)

    if (functionBody.trim.isEmpty) {
      Future.successful(Unit)
    } else {
      val functionCode =
        new FunctionCode().
          withZipFile(getZipFor(behaviorVersion, functionBody, params, maybeAWSConfig, requiredOAuth2Applications))
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
