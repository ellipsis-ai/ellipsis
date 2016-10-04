package services

import java.io.{File, PrintWriter}
import java.nio.ByteBuffer
import java.nio.file.{Files, Paths}
import javax.inject.Inject

import com.amazonaws.services.lambda.AWSLambdaAsyncClient
import com.amazonaws.services.lambda.model._
import models.Models
import models.behaviors._
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.config.awsconfig.AWSConfig
import models.behaviors.config.requiredoauth2apiconfig.RequiredOAuth2ApiConfig
import models.behaviors.events.MessageEvent
import models.environmentvariable.EnvironmentVariable
import models.behaviors.invocationtoken.InvocationToken
import models.team.Team
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.libs.json._
import play.api.libs.ws.WSClient
import sun.misc.BASE64Decoder
import utils.JavaFutureConverter

import scala.collection.JavaConversions.asScalaBuffer
import scala.concurrent.Future
import scala.reflect.io.Path
import sys.process._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class AWSLambdaServiceImpl @Inject() (
                                       val configuration: Configuration,
                                       val models: Models,
                                       val ws: WSClient,
                                       val cache: CacheApi,
                                       val dataService: DataService,
                                       val logsService: AWSLogsService
                                       ) extends AWSLambdaService {

  import AWSLambdaConstants._

  val client: AWSLambdaAsyncClient = new AWSLambdaAsyncClient(credentials)
  val apiBaseUrl: String = configuration.getString(s"application.$API_BASE_URL_KEY").get

  def fetchFunctions(maybeNextMarker: Option[String]): Future[List[FunctionConfiguration]] = {
    val listRequest = new ListFunctionsRequest()
    val listRequestWithMarker = maybeNextMarker.map { nextMarker =>
      listRequest.withMarker(nextMarker)
    }.getOrElse(listRequest)
    JavaFutureConverter.javaToScala(client.listFunctionsAsync(listRequestWithMarker)).flatMap { result =>
      if (result.getNextMarker == null) {
        Future.successful(List())
      } else {
        fetchFunctions(Some(result.getNextMarker)).map { functions =>
          (result.getFunctions ++ functions).toList
        }
      }
    }
  }

  def listFunctionNames: Future[Seq[String]] = {
    fetchFunctions(None).map { functions =>
      functions.map(_.getFunctionName)
    }
  }

  private def contextParamDataFor(
                                   environmentVariables: Seq[EnvironmentVariable],
                                   userInfo: UserInfo,
                                   teamInfo: TeamInfo,
                                   token: InvocationToken
                                   ): Seq[(String, JsObject)] = {
    Seq(CONTEXT_PARAM -> JsObject(Seq(
      API_BASE_URL_KEY -> JsString(apiBaseUrl),
      TOKEN_KEY -> JsString(token.id),
      ENV_KEY -> JsObject(environmentVariables.map { ea =>
        ea.name -> JsString(ea.value)
      }),
      USER_INFO_KEY -> userInfo.toJson,
      TEAM_INFO_KEY -> teamInfo.toJson
    )))
  }

  def invokeFunction(
                      functionName: String,
                      payloadData: Seq[(String, JsValue)],
                      team: Team,
                      event: MessageEvent,
                      requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfig],
                      environmentVariables: Seq[EnvironmentVariable],
                      successFn: InvokeResult => BotResult
                    ): Future[BotResult] = {
    for {
      token <- dataService.invocationTokens.createFor(team)
      userInfo <- event.context.userInfo(ws, dataService)
      notReadyOAuth2Applications <- Future.successful(requiredOAuth2ApiConfigs.filterNot(_.isReady))
      missingOAuth2Applications <- Future.successful(requiredOAuth2ApiConfigs.flatMap(_.maybeApplication).filter { app =>
        !userInfo.links.exists(_.externalSystem == app.name)
      })
      result <- notReadyOAuth2Applications.headOption.map { firstNotReadyOAuth2App =>
        Future.successful(RequiredApiNotReady(firstNotReadyOAuth2App, event, cache, configuration))
      }.getOrElse {
        val (missingOAuth2ApplicationsRequiringAuth, otherMissingOAuth2Applications) =
          missingOAuth2Applications.partition(_.api.grantType.requiresAuth)
        missingOAuth2ApplicationsRequiringAuth.headOption.map { firstMissingOAuth2App =>
          event.context.ensureUser(dataService).flatMap { user =>
            dataService.loginTokens.createFor(user).map { loginToken =>
              OAuth2TokenMissing(firstMissingOAuth2App, event, loginToken, cache, configuration)
            }
          }
        }.getOrElse {
          TeamInfo.forOAuth2Apps(otherMissingOAuth2Applications, team, ws).flatMap { teamInfo =>
            val payloadJson = JsObject(
              payloadData ++ contextParamDataFor(environmentVariables, userInfo, teamInfo, token)
            )
            val invokeRequest =
              new InvokeRequest().
                withLogType(LogType.Tail).
                withFunctionName(functionName).
                withInvocationType(InvocationType.RequestResponse).
                withPayload(payloadJson.toString())
            JavaFutureConverter.javaToScala(client.invokeAsync(invokeRequest)).map(successFn).recover {
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
    } yield result
  }

  def invoke(
              behaviorVersion: BehaviorVersion,
              parametersWithValues: Seq[ParameterWithValue],
              environmentVariables: Seq[EnvironmentVariable],
              event: MessageEvent
              ): Future[BotResult] = {
    for {
      missingEnvVars <- dataService.behaviorVersions.missingEnvironmentVariablesIn(behaviorVersion, environmentVariables, dataService)
      requiredOAuth2ApiConfigs <- dataService.requiredOAuth2ApiConfigs.allFor(behaviorVersion)
      result <- if (missingEnvVars.nonEmpty) {
        Future.successful(MissingEnvVarsResult(behaviorVersion, configuration, missingEnvVars))
      } else if (behaviorVersion.functionBody.isEmpty) {
        Future.successful(SuccessResult(JsNull, parametersWithValues, behaviorVersion.maybeResponseTemplate, None))
      } else {
        invokeFunction(
          behaviorVersion.functionName,
          parametersWithValues.map { ea => (ea.invocationName, ea.preparedValue) },
          behaviorVersion.team,
          event,
          requiredOAuth2ApiConfigs,
          environmentVariables,
          result => {
            val logString = new java.lang.String(new BASE64Decoder().decodeBuffer(result.getLogResult))
            val logResult = AWSLambdaLogResult.fromText(logString)
            behaviorVersion.resultFor(result.getPayload, logResult, parametersWithValues, configuration)
          }
        )
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
         |
         | $CONTEXT_PARAM.AWS = AWS;
       """.stripMargin
    }.getOrElse("")
  }

  private def accessTokenCodeFor(app: RequiredOAuth2ApiConfig): String = {
    app.maybeApplication.map { application =>
      val infoKey =  if (application.api.grantType.requiresAuth) { "userInfo" } else { "teamInfo" }
      s"""$CONTEXT_PARAM.accessTokens.${application.keyName} = event.$CONTEXT_PARAM.$infoKey.links.find((ea) => ea.externalSystem == "${application.name}").oauthToken;"""
    }.getOrElse("")
  }

  private def accessTokensCodeFor(requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfig]): String = {
    requiredOAuth2ApiConfigs.map(accessTokenCodeFor).mkString("\n")
  }

  def functionWithParams(params: Array[String], functionBody: String): String = {
    s"""function(${(params ++ Array(CONTEXT_PARAM)).mkString(", ")}) {
        |  $functionBody
        |}\n""".stripMargin
  }

  private def nodeCodeFor(functionBody: String, params: Array[String], maybeAwsConfig: Option[AWSConfig], requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfig]): String = {
    val paramsFromEvent = params.indices.map(i => s"event.${invocationParamFor(i)}")
    val invocationParamsString = (paramsFromEvent ++ Array(s"event.$CONTEXT_PARAM")).mkString(", ")

    // Note: this attempts to make line numbers in the lambda script line up with those displayed in the UI
    // Be careful changing either this or the UI line numbers
    s"""exports.handler = function(event, context, callback) { var fn = ${functionWithParams(params, functionBody)};
        |   var $CONTEXT_PARAM = event.$CONTEXT_PARAM;
        |   $CONTEXT_PARAM.$NO_RESPONSE_KEY = function() {
        |     callback(null, { $NO_RESPONSE_KEY: true });
        |   };
        |   $CONTEXT_PARAM.success = function(result) {
        |     callback(null, { "result": result === undefined ? null : result });
        |   };
        |   $CONTEXT_PARAM.error = function(err) { callback(err); };
        |   ${awsCodeFor(maybeAwsConfig)}
        |   $CONTEXT_PARAM.accessTokens = {};
        |   ${accessTokensCodeFor(requiredOAuth2ApiConfigs)}
        |   fn($invocationParamsString);
        |}
    """.stripMargin
  }

  private def dirNameFor(functionName: String) = s"/tmp/$functionName"
  private def zipFileNameFor(functionName: String) = s"${dirNameFor(functionName)}.zip"

  private def createZipWithModulesFor(
                                       functionName: String,
                                       functionBody: String,
                                       params: Array[String],
                                       maybeAWSConfig: Option[AWSConfig],
                                       requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfig]
                                     ): Unit = {
    val dirName = dirNameFor(functionName)
    val path = Path(dirName)
    path.createDirectory()

    val writer = new PrintWriter(new File(s"$dirName/index.js"))
    writer.write(nodeCodeFor(functionBody, params, maybeAWSConfig, requiredOAuth2ApiConfigs))
    writer.close()

    requiredModulesIn(functionBody).foreach { moduleName =>
      // NPM wants to write a lockfile in $HOME; this makes it work for daemons
      Process(Seq("bash","-c",s"cd $dirName && npm install $moduleName"), None, "HOME" -> "/tmp").!
    }

    Process(Seq("bash","-c",s"cd $dirName && zip -r ${zipFileNameFor(functionName)} *")).!
  }

  private def getZipFor(
                         functionName: String,
                         functionBody: String,
                         params: Array[String],
                         maybeAWSConfig: Option[AWSConfig],
                         requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfig]
                       ): ByteBuffer = {
    createZipWithModulesFor(functionName, functionBody, params, maybeAWSConfig, requiredOAuth2ApiConfigs)
    val path = Paths.get(zipFileNameFor(functionName))
    ByteBuffer.wrap(Files.readAllBytes(path))
  }

  def deleteFunction(functionName: String): Future[Unit] = {
    val deleteFunctionRequest =
      new DeleteFunctionRequest().withFunctionName(functionName)
    val eventuallyDeleteFunction = Future {
      try {
        client.deleteFunction(deleteFunctionRequest)
      } catch {
        case e: ResourceNotFoundException => // we expect this when creating the first time
      }
    }
    val eventuallyDeleteLogGroup = logsService.deleteGroupForLambdaFunctionNamed(functionName)
    for {
      _ <- eventuallyDeleteFunction
      _ <- eventuallyDeleteLogGroup
    } yield {}
  }

  def deployFunction(
                      functionName: String,
                      functionBody: String,
                      params: Array[String],
                      maybeAWSConfig: Option[AWSConfig],
                      requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfig]
                    ): Future[Unit] = {

    deleteFunction(functionName).andThen {
      case Failure(t) => Future.successful(Unit)
      case Success(v) => if (functionBody.trim.isEmpty) {
        Future.successful(Unit)
      } else {
        val functionCode =
          new FunctionCode().
            withZipFile(getZipFor(functionName, functionBody, params, maybeAWSConfig, requiredOAuth2ApiConfigs))
        val createFunctionRequest =
          new CreateFunctionRequest().
            withFunctionName(functionName).
            withCode(functionCode).
            withRole(configuration.getString("aws.role").get).
            withRuntime(com.amazonaws.services.lambda.model.Runtime.Nodejs43).
            withHandler("index.handler").
            withTimeout(INVOCATION_TIMEOUT_SECONDS)

        JavaFutureConverter.javaToScala(client.createFunctionAsync(createFunctionRequest)).map(_ => Unit)
      }
    }
  }

  def deployFunctionFor(
                         behaviorVersion: BehaviorVersion,
                         functionBody: String,
                         params: Array[String],
                         maybeAWSConfig: Option[AWSConfig],
                         requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfig]
                         ): Future[Unit] = {
    deployFunction(behaviorVersion.functionName, functionBody, params, maybeAWSConfig, requiredOAuth2ApiConfigs)
  }
}
