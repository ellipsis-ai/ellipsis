package services

import java.io.{File, PrintWriter}
import java.nio.ByteBuffer
import java.nio.file.{Files, Paths}
import javax.inject.Inject

import akka.actor.ActorSystem
import com.amazonaws.services.lambda.AWSLambdaAsyncClient
import com.amazonaws.services.lambda.model._
import models.Models
import models.behaviors._
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.config.awsconfig.AWSConfig
import models.behaviors.config.requiredoauth2apiconfig.RequiredOAuth2ApiConfig
import models.behaviors.config.requiredsimpletokenapi.RequiredSimpleTokenApi
import models.behaviors.events.Event
import models.environmentvariable.{EnvironmentVariable, TeamEnvironmentVariable, UserEnvironmentVariable}
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
                                       val logsService: AWSLogsService,
                                       implicit val actorSystem: ActorSystem
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

  def partionedFunctionNames: Future[PartitionedFunctionNames] = {
    for {
      allFunctionNames <- listFunctionNames
      currentVersionIdsWithFunction <- dataService.behaviorVersions.currentIdsWithFunction
    } yield {
      val missing = currentVersionIdsWithFunction.diff(allFunctionNames)
      val current = currentVersionIdsWithFunction.intersect(allFunctionNames)
      val obsolete = allFunctionNames.diff(currentVersionIdsWithFunction)
      PartitionedFunctionNames(current, missing, obsolete)
    }
  }

  private def contextParamDataFor(
                                   environmentVariables: Seq[EnvironmentVariable],
                                   userInfo: UserInfo,
                                   teamInfo: TeamInfo,
                                   token: InvocationToken
                                   ): Seq[(String, JsObject)] = {
    val teamEnvVars = environmentVariables.filter(ev => ev.isInstanceOf[TeamEnvironmentVariable])
    val userEnvVars = environmentVariables.filter(ev => ev.isInstanceOf[UserEnvironmentVariable])
    Seq(CONTEXT_PARAM -> JsObject(Seq(
      API_BASE_URL_KEY -> JsString(apiBaseUrl),
      TOKEN_KEY -> JsString(token.id),
      ENV_KEY -> JsObject(teamEnvVars.map { ea =>
        ea.name -> JsString(ea.value)
      }),
      USER_ENV_KEY -> JsObject(userEnvVars.map { ea =>
        ea.name -> JsString(ea.value)
      }),
      USER_INFO_KEY -> userInfo.toJson,
      TEAM_INFO_KEY -> teamInfo.toJson
    )))
  }

  def invokeFunction(
                      functionName: String,
                      token: InvocationToken,
                      payloadData: Seq[(String, JsValue)],
                      team: Team,
                      event: Event,
                      requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfig],
                      environmentVariables: Seq[EnvironmentVariable],
                      successFn: InvokeResult => BotResult
                    ): Future[BotResult] = {
    for {
      userInfo <- event.userInfo(ws, dataService)
      result <- {
        val oauth2ApplicationsNeedingRefresh =
          requiredOAuth2ApiConfigs.flatMap(_.maybeApplication).
            filter { app =>
              !userInfo.links.exists(_.externalSystem == app.name)
            }.
            filterNot(_.api.grantType.requiresAuth)
        TeamInfo.forOAuth2Apps(oauth2ApplicationsNeedingRefresh, team, ws).flatMap { teamInfo =>
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
                case amazonServiceExceptionRegex() => AWSDownResult(event)
                case _ => throw e
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
              event: Event
              ): Future[BotResult] = {
    for {
      requiredOAuth2ApiConfigs <- dataService.requiredOAuth2ApiConfigs.allFor(behaviorVersion)
      result <- if (behaviorVersion.functionBody.isEmpty) {
        Future.successful(SuccessResult(event, JsNull, parametersWithValues, behaviorVersion.maybeResponseTemplate, None, behaviorVersion.forcePrivateResponse))
      } else {
        for {
          user <- event.ensureUser(dataService)
          token <- dataService.invocationTokens.createFor(user, behaviorVersion.behavior, event.maybeScheduled)
          invocationResult <- invokeFunction(
            behaviorVersion.functionName,
            token,
            parametersWithValues.map { ea => (ea.invocationName, ea.preparedValue) },
            behaviorVersion.team,
            event,
            requiredOAuth2ApiConfigs,
            environmentVariables,
            result => {
              val logString = new java.lang.String(new BASE64Decoder().decodeBuffer(result.getLogResult))
              val logResult = AWSLambdaLogResult.fromText(logString)
              behaviorVersion.resultFor(result.getPayload, logResult, parametersWithValues, dataService, configuration, event)
            }
          )
        } yield invocationResult
      }
    } yield result
  }

  val amazonServiceExceptionRegex = """.*com\.amazonaws\.AmazonServiceException.*""".r

  val requireRegex = """.*require\(['"]\s*(\S+)\s*['"]\).*""".r

  val alreadyIncludedModules = Array("aws-sdk", "dynamodb-doc")

  private def requiredModulesIn(code: String): Array[String] = {
    requireRegex.findAllMatchIn(code).flatMap(_.subgroups.headOption).toArray.diff(alreadyIncludedModules).sorted
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
      s"""$CONTEXT_PARAM.accessTokens.${application.keyName} = event.$CONTEXT_PARAM.$infoKey.links.find((ea) => ea.externalSystem == "${application.name}").token;"""
    }.getOrElse("")
  }

  private def accessTokensCodeFor(requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfig]): String = {
    requiredOAuth2ApiConfigs.map(accessTokenCodeFor).mkString("\n")
  }

  private def accessTokenCodeFor(required: RequiredSimpleTokenApi): String = {
    val api = required.api
    s"""$CONTEXT_PARAM.accessTokens.${api.keyName} = event.$CONTEXT_PARAM.userInfo.links.find((ea) => ea.externalSystem == "${api.name}").token;"""
  }

  private def simpleTokensCodeFor(requiredSimpleTokenApis: Seq[RequiredSimpleTokenApi]): String = {
    requiredSimpleTokenApis.map(accessTokenCodeFor).mkString("\n")
  }

  def functionWithParams(params: Array[String], functionBody: String): String = {
    s"""function(${(params ++ Array(CONTEXT_PARAM)).mkString(", ")}) {
        |  ${functionBody.trim}
        |}\n""".stripMargin
  }

  private def nodeCodeFor(
                           functionBody: String,
                           params: Array[String],
                           maybeAwsConfig: Option[AWSConfig],
                           requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfig],
                           requiredSimpleTokenApis: Seq[RequiredSimpleTokenApi]
                         ): String = {
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
        |   $CONTEXT_PARAM.error = function(err) { callback(err || "(No error message or an empty error message was provided.)"); };
        |   ${awsCodeFor(maybeAwsConfig)}
        |   $CONTEXT_PARAM.accessTokens = {};
        |   ${accessTokensCodeFor(requiredOAuth2ApiConfigs)}
        |   ${simpleTokensCodeFor(requiredSimpleTokenApis)}
        |   fn($invocationParamsString);
        |}
    """.stripMargin
  }

  private def dirNameFor(functionName: String) = s"/tmp/$functionName"
  private def zipFileNameFor(functionName: String) = s"${dirNameFor(functionName)}.zip"

  case class PreviousFunctionInfo(functionName: String, functionBody: String) {
    val requiredModules = requiredModulesIn(functionBody)
    val dirName = dirNameFor(functionName)

    def canCopyModules(neededModules: Array[String]): Boolean = {
      requiredModules.sameElements(neededModules) &&
        Files.exists(Paths.get(dirName))
    }

    def copyModulesInto(destinationDirName: String) = {
      Process(Seq("bash","-c",s"cp -r $dirName/node_modules $destinationDirName/"), None, "HOME" -> "/tmp").!
    }
  }

  private def createZipWithModulesFor(
                                       functionName: String,
                                       functionBody: String,
                                       params: Array[String],
                                       maybeAWSConfig: Option[AWSConfig],
                                       requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfig],
                                       requiredSimpleTokenApis: Seq[RequiredSimpleTokenApi],
                                       maybePreviousFunctionInfo: Option[PreviousFunctionInfo]
                                     ): Unit = {
    val dirName = dirNameFor(functionName)
    val path = Path(dirName)
    path.createDirectory()

    val writer = new PrintWriter(new File(s"$dirName/index.js"))
    writer.write(nodeCodeFor(functionBody, params, maybeAWSConfig, requiredOAuth2ApiConfigs, requiredSimpleTokenApis))
    writer.close()

    val requiredModules = requiredModulesIn(functionBody)
    val shouldInstallModules = maybePreviousFunctionInfo.forall { previousFunctionInfo =>
      if (previousFunctionInfo.canCopyModules(requiredModules)) {
        previousFunctionInfo.copyModulesInto(dirName)
        false
      } else {
        true
      }
    }
    if (shouldInstallModules) {
      requiredModules.foreach { moduleName =>
        // NPM wants to write a lockfile in $HOME; this makes it work for daemons
        Process(Seq("bash","-c",s"cd $dirName && npm install $moduleName"), None, "HOME" -> "/tmp").!
      }
    }

    Process(Seq("bash","-c",s"cd $dirName && zip -q -r ${zipFileNameFor(functionName)} *")).!
  }

  private def getZipFor(
                         functionName: String,
                         functionBody: String,
                         params: Array[String],
                         maybeAWSConfig: Option[AWSConfig],
                         requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfig],
                         requiredSimpleTokenApis: Seq[RequiredSimpleTokenApi],
                         maybePreviousFunctionInfo: Option[PreviousFunctionInfo]
                       ): ByteBuffer = {
    createZipWithModulesFor(functionName, functionBody, params, maybeAWSConfig, requiredOAuth2ApiConfigs, requiredSimpleTokenApis, maybePreviousFunctionInfo)
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
                      requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfig],
                      requiredSimpleTokenApis: Seq[RequiredSimpleTokenApi],
                      maybePreviousFunctionInfo: Option[PreviousFunctionInfo]
                    ): Future[Unit] = {

    deleteFunction(functionName).andThen {
      case Failure(t) => Future.successful(Unit)
      case Success(v) => if (functionBody.trim.isEmpty) {
        Future.successful(Unit)
      } else {
        val functionCode =
          new FunctionCode().
            withZipFile(getZipFor(functionName, functionBody, params, maybeAWSConfig, requiredOAuth2ApiConfigs, requiredSimpleTokenApis, maybePreviousFunctionInfo))
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
                         requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfig],
                         requiredSimpleTokenApis: Seq[RequiredSimpleTokenApi]
                       ): Future[Unit] = {
    dataService.behaviorVersions.maybePreviousFor(behaviorVersion).map { maybePrevious =>
      val maybePreviousFunctionInfo = maybePrevious.map { version =>
        PreviousFunctionInfo(version.functionName, version.functionBody)
      }
      deployFunction(behaviorVersion.functionName, functionBody, params, maybeAWSConfig, requiredOAuth2ApiConfigs, requiredSimpleTokenApis, maybePreviousFunctionInfo)
    }

  }
}
