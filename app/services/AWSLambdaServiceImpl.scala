package services

import java.io.{File, PrintWriter}
import java.nio.ByteBuffer
import java.nio.file.{Files, Paths}
import javax.inject.Inject

import akka.actor.ActorSystem
import com.amazonaws.services.lambda.model._
import com.amazonaws.services.lambda.{AWSLambdaAsync, AWSLambdaAsyncClientBuilder}
import json.Formatting._
import json.NodeModuleVersionData
import models.Models
import models.behaviors._
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.config.awsconfig.AWSConfig
import models.behaviors.config.requiredoauth2apiconfig.RequiredOAuth2ApiConfig
import models.behaviors.config.requiredsimpletokenapi.RequiredSimpleTokenApi
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import models.behaviors.invocationtoken.InvocationToken
import models.behaviors.library.LibraryVersion
import models.behaviors.nodemoduleversion.NodeModuleVersion
import models.environmentvariable.{EnvironmentVariable, TeamEnvironmentVariable, UserEnvironmentVariable}
import models.team.Team
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}
import slick.dbio.DBIO
import sun.misc.BASE64Decoder
import utils.JavaFutureConverter

import scala.collection.JavaConversions.asScalaBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.reflect.io.Path
import scala.sys.process._
import scala.util.{Failure, Success}

class AWSLambdaServiceImpl @Inject() (
                                       val configuration: Configuration,
                                       val models: Models,
                                       val ws: WSClient,
                                       val dataService: DataService,
                                       val logsService: AWSLogsService,
                                       implicit val actorSystem: ActorSystem
                                       ) extends AWSLambdaService {

  import AWSLambdaConstants._

  val client: AWSLambdaAsync =
    AWSLambdaAsyncClientBuilder.standard().
      withRegion(region).
      withCredentials(credentialsProvider).
      build()

  val apiBaseUrl: String = configuration.getString(s"application.$API_BASE_URL_KEY").get

  def fetchFunctions(maybeNextMarker: Option[String]): Future[List[FunctionConfiguration]] = {
    val listRequest = new ListFunctionsRequest()
    val listRequestWithMarker = maybeNextMarker.map { nextMarker =>
      listRequest.withMarker(nextMarker)
    }.getOrElse(listRequest)
    JavaFutureConverter.javaToScala(client.listFunctionsAsync(listRequestWithMarker)).flatMap { result =>
      if (result.getNextMarker == null) {
        Future.successful(result.getFunctions.toList)
      } else {
        fetchFunctions(Some(result.getNextMarker)).map { functions =>
          (result.getFunctions ++ functions).toList
        }
      }
    }
  }

  def listBehaviorFunctionNames: Future[Seq[String]] = {
    fetchFunctions(None).map { functions =>
      val allFunctionNames = functions.map(_.getFunctionName)
      val behaviorFunctionNames = allFunctionNames.filter { ea =>
        ea.startsWith(BehaviorVersion.lambdaFunctionPrefix)
      }
      behaviorFunctionNames
    }
  }

  def partionedBehaviorFunctionNames: Future[PartitionedFunctionNames] = {
    for {
      allBehaviorFunctionNames <- listBehaviorFunctionNames
      currentFunctionNames <- dataService.behaviorVersions.currentFunctionNames
    } yield {
      val missing = currentFunctionNames.diff(allBehaviorFunctionNames)
      val current = currentFunctionNames.intersect(allBehaviorFunctionNames)
      val obsolete = allBehaviorFunctionNames.diff(currentFunctionNames)
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

  def invokeFunctionAction(
                            functionName: String,
                            token: InvocationToken,
                            payloadData: Seq[(String, JsValue)],
                            team: Team,
                            event: Event,
                            requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfig],
                            environmentVariables: Seq[EnvironmentVariable],
                            successFn: InvokeResult => BotResult,
                            maybeConversation: Option[Conversation],
                            isRetrying: Boolean
                          ): DBIO[BotResult] = {
    for {
      userInfo <- event.userInfoAction(ws, dataService)
      result <- {
        val oauth2ApplicationsNeedingRefresh =
          requiredOAuth2ApiConfigs.flatMap(_.maybeApplication).
            filter { app =>
              !userInfo.links.exists(_.externalSystem == app.name)
            }.
            filterNot(_.api.grantType.requiresAuth)
        DBIO.from(TeamInfo.forOAuth2Apps(oauth2ApplicationsNeedingRefresh, team, ws).flatMap { teamInfo =>
          val payloadJson = JsObject(
            payloadData ++ contextParamDataFor(environmentVariables, userInfo, teamInfo, token)
          )
          val invokeRequest =
            new InvokeRequest().
              withLogType(LogType.Tail).
              withFunctionName(functionName).
              withInvocationType(InvocationType.RequestResponse).
              withPayload(payloadJson.toString())
          JavaFutureConverter.javaToScala(client.invokeAsync(invokeRequest)).map(successFn).recoverWith {
            case e: java.util.concurrent.ExecutionException => {
              e.getMessage match {
                case amazonServiceExceptionRegex() => Future.successful(AWSDownResult(event, maybeConversation))
                case resourceNotFoundExceptionRegex() => {
                  if (!isRetrying) {
                    Logger.info(s"retrying behavior invocation after resource not found")
                    Thread.sleep(2000)
                    dataService.run(invokeFunctionAction(functionName, token, payloadData, team, event, requiredOAuth2ApiConfigs, environmentVariables, successFn, maybeConversation, isRetrying=true))
                  } else {
                    throw e
                  }
                }
                case _ => throw e
              }
            }
          }
        })
      }
    } yield result
  }

  def invokeAction(
                    behaviorVersion: BehaviorVersion,
                    parametersWithValues: Seq[ParameterWithValue],
                    environmentVariables: Seq[EnvironmentVariable],
                    event: Event,
                    maybeConversation: Option[Conversation]
                  ): DBIO[BotResult] = {
    for {
      requiredOAuth2ApiConfigs <- dataService.requiredOAuth2ApiConfigs.allForAction(behaviorVersion.groupVersion)
      result <- if (behaviorVersion.functionBody.isEmpty) {
        DBIO.successful(SuccessResult(event, maybeConversation, JsNull, JsNull, parametersWithValues, behaviorVersion.maybeResponseTemplate, None, behaviorVersion.forcePrivateResponse))
      } else {
        for {
          user <- event.ensureUserAction(dataService)
          token <- dataService.invocationTokens.createForAction(user, behaviorVersion.behavior, event.maybeScheduled)
          invocationResult <- invokeFunctionAction(
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
              behaviorVersion.resultFor(result.getPayload, logResult, parametersWithValues, dataService, configuration, event, maybeConversation)
            },
            maybeConversation,
            isRetrying = false
          )
        } yield invocationResult
      }
    } yield result
  }

  val amazonServiceExceptionRegex = """.*com\.amazonaws\.AmazonServiceException.*""".r
  val resourceNotFoundExceptionRegex = """com\.amazonaws\.services\.lambda\.model\.ResourceNotFoundException.*""".r

  val requireRegex = """.*require\s*\(['"]\s*(\S+)\s*['"]\).*""".r

  val alreadyIncludedModules = Array("aws-sdk", "dynamodb-doc")

  private def requiredModulesIn(code: String, libraries: Seq[LibraryVersion], includeLibraryRequires: Boolean): Array[String] = {
    val libraryNames = libraries.map(_.name)
    val requiredForCode =
      requireRegex.findAllMatchIn(code).
        flatMap(_.subgroups.headOption).
        toArray.
        diff(alreadyIncludedModules ++ libraryNames).
        sorted
    val requiredForLibs = if (includeLibraryRequires) {
      libraries.flatMap(ea => requiredModulesIn(ea.functionBody, libraries, includeLibraryRequires = false))
    } else {
      Seq()
    }
    (requiredForCode ++ requiredForLibs).distinct
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
    s"""exports.handler = function(event, context, callback) { var fn = ${functionWithParams(params, functionBody)}
       |  var $CONTEXT_PARAM = event.$CONTEXT_PARAM;
       |  const log = [];
       |  $OVERRIDE_CONSOLE
       |  $CONTEXT_PARAM.$NO_RESPONSE_KEY = $NO_RESPONSE_CALLBACK_FUNCTION
       |  $CONTEXT_PARAM.success = $SUCCESS_CALLBACK_FUNCTION
       |  $ERROR_CLASS
       |  $CONTEXT_PARAM.Error = EllipsisError;
       |  $CONTEXT_PARAM.error = $ERROR_CALLBACK_FUNCTION
       |
       |  process.once('unhandledRejection', $CONTEXT_PARAM.error);
       |
       |  ${awsCodeFor(maybeAwsConfig)}
       |  $CONTEXT_PARAM.accessTokens = {};
       |  ${accessTokensCodeFor(requiredOAuth2ApiConfigs)}
       |  ${simpleTokensCodeFor(requiredSimpleTokenApis)}
       |
       |  try {
       |    fn($invocationParamsString);
       |  } catch(err) {
       |    $CONTEXT_PARAM.error(err);
       |  }
       |}
    """.stripMargin
  }

  private def dirNameFor(functionName: String) = s"/tmp/$functionName"
  private def zipFileNameFor(functionName: String) = s"${dirNameFor(functionName)}.zip"

  case class PreviousFunctionInfo(functionName: String, functionBody: String, libraries: Seq[LibraryVersion]) {
    val requiredModules = requiredModulesIn(functionBody, libraries, includeLibraryRequires = true)
    val dirName = dirNameFor(functionName)

    def canCopyModules(neededModules: Array[String]): Boolean = {
      requiredModules.sameElements(neededModules) &&
        Files.exists(Paths.get(dirName))
    }

    def copyModulesInto(destinationDirName: String) = {
      Process(Seq("bash","-c",s"cp -r $dirName/node_modules $destinationDirName/"), None, "HOME" -> "/tmp").!
    }
  }

  private def writeFileNamed(path: String, content: String) = {
    val writer = new PrintWriter(new File(path))
    writer.write(content)
    writer.close()
  }

  private def createZipWithModulesFor(
                                       functionName: String,
                                       functionBody: String,
                                       params: Array[String],
                                       libraries: Seq[LibraryVersion],
                                       maybeAWSConfig: Option[AWSConfig],
                                       requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfig],
                                       requiredSimpleTokenApis: Seq[RequiredSimpleTokenApi],
                                       maybePreviousFunctionInfo: Option[PreviousFunctionInfo],
                                       forceNodeModuleUpdate: Boolean
                                     ): Unit = {
    val dirName = dirNameFor(functionName)
    val path = Path(dirName)
    path.createDirectory()

    writeFileNamed(s"$dirName/index.js", nodeCodeFor(functionBody, params, maybeAWSConfig, requiredOAuth2ApiConfigs, requiredSimpleTokenApis))
    libraries.foreach { ea =>
      writeFileNamed(s"$dirName/${ea.jsName}", ea.code)
    }

    val requiredModules = requiredModulesIn(functionBody, libraries, includeLibraryRequires = true)
    val canUseCopyModules = maybePreviousFunctionInfo.forall { previousFunctionInfo =>
      if (previousFunctionInfo.canCopyModules(requiredModules)) {
        previousFunctionInfo.copyModulesInto(dirName)
        true
      } else {
        false
      }
    }
    if (forceNodeModuleUpdate || !canUseCopyModules) {
      requiredModules.foreach { moduleName =>
        // NPM wants to write a lockfile in $HOME; this makes it work for daemons
        Process(Seq("bash","-c",s"cd $dirName && npm install $moduleName"), None, "HOME" -> "/tmp").!
      }
    }

    Process(Seq("bash","-c",s"cd $dirName && zip -q -r ${zipFileNameFor(functionName)} *")).!
  }

  private def getNodeModuleInfoFor(functionName: String): JsValue = {
    val dirName = dirNameFor(functionName)
    val infoString = try {
      Process(Seq("bash","-c",s"cd $dirName && npm list --depth=0 --json=true")).!!
    } catch {
      case t: Throwable => "{}"
    }
    Json.parse(infoString)
  }

  def ensureNodeModuleVersionsFor(behaviorVersion: BehaviorVersion): DBIO[Seq[NodeModuleVersion]] = {
    val json = getNodeModuleInfoFor(behaviorVersion.functionName)
    val maybeDependencies = (json \ "dependencies").asOpt[JsObject]
    maybeDependencies.map { dependencies =>
      DBIO.sequence(dependencies.values.toSeq.map { depJson =>
        depJson.validate[NodeModuleVersionData] match {
          case JsSuccess(info, _) => {
            dataService.nodeModuleVersions.ensureForAction(info.from, info.version, behaviorVersion.groupVersion).map(Some(_))
          }
          case JsError(err) => DBIO.successful(None)
        }
      }).map(_.flatten)
    }.getOrElse(DBIO.successful(Seq()))
  }

  private def getZipFor(
                         functionName: String,
                         functionBody: String,
                         params: Array[String],
                         libraries: Seq[LibraryVersion],
                         maybeAWSConfig: Option[AWSConfig],
                         requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfig],
                         requiredSimpleTokenApis: Seq[RequiredSimpleTokenApi],
                         maybePreviousFunctionInfo: Option[PreviousFunctionInfo],
                         forceNodeModuleUpdate: Boolean
                       ): ByteBuffer = {
    createZipWithModulesFor(
      functionName,
      functionBody,
      params,
      libraries,
      maybeAWSConfig,
      requiredOAuth2ApiConfigs,
      requiredSimpleTokenApis,
      maybePreviousFunctionInfo,
      forceNodeModuleUpdate
    )
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
                      libraries: Seq[LibraryVersion],
                      maybeAWSConfig: Option[AWSConfig],
                      requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfig],
                      requiredSimpleTokenApis: Seq[RequiredSimpleTokenApi],
                      maybePreviousFunctionInfo: Option[PreviousFunctionInfo],
                      forceNodeModuleUpdate: Boolean
                    ): Future[Unit] = {

    deleteFunction(functionName).andThen {
      case Failure(t) => Future.successful(Unit)
      case Success(v) => if (functionBody.trim.isEmpty) {
        Future.successful(Unit)
      } else {
        val functionCode =
          new FunctionCode().
            withZipFile(getZipFor(
              functionName,
              functionBody,
              params,
              libraries,
              maybeAWSConfig,
              requiredOAuth2ApiConfigs,
              requiredSimpleTokenApis,
              maybePreviousFunctionInfo,
              forceNodeModuleUpdate
            ))
        val createFunctionRequest =
          new CreateFunctionRequest().
            withFunctionName(functionName).
            withCode(functionCode).
            withRole(configuration.getString("aws.role").get).
            withRuntime(com.amazonaws.services.lambda.model.Runtime.Nodejs610).
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
                         libraries: Seq[LibraryVersion],
                         maybeAWSConfig: Option[AWSConfig],
                         requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfig],
                         requiredSimpleTokenApis: Seq[RequiredSimpleTokenApi],
                         forceNodeModuleUpdate: Boolean
                       ): Future[Unit] = {
    for {
      maybePrevious <- dataService.behaviorVersions.maybePreviousFor(behaviorVersion)
      previousLibraries <- maybePrevious.map { prev =>
        dataService.libraries.allFor(prev.groupVersion)
      }.getOrElse(Future.successful(Seq()))
      maybePreviousFunctionInfo <- Future.successful(maybePrevious.map { version =>
        PreviousFunctionInfo(version.functionName, version.functionBody, previousLibraries)
      })
      _ <- deployFunction(
        behaviorVersion.functionName,
        functionBody,
        params,
        libraries,
        maybeAWSConfig,
        requiredOAuth2ApiConfigs,
        requiredSimpleTokenApis,
        maybePreviousFunctionInfo,
        forceNodeModuleUpdate
      )
    } yield {}
  }
}
