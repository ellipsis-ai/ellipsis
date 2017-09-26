package services

import java.io.{File, PrintWriter}
import java.nio.ByteBuffer
import java.nio.file.{Files, Paths}
import javax.inject.Inject

import akka.actor.ActorSystem
import com.amazonaws.services.lambda.model._
import com.amazonaws.services.lambda.{AWSLambdaAsync, AWSLambdaAsyncClientBuilder}
import com.amazonaws.services.logs.model.{CreateLogGroupRequest, PutSubscriptionFilterRequest}
import com.amazonaws.services.logs.{AWSLogsAsync, AWSLogsAsyncClientBuilder}
import json.Formatting._
import json.NodeModuleVersionData
import models.behaviors._
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.config.requiredawsconfig.RequiredAWSConfig
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
import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.reflect.io.Path
import scala.sys.process._
import scala.util.{Failure, Success}

class AWSLambdaServiceImpl @Inject() (
                                       val configuration: Configuration,
                                       val ws: WSClient,
                                       val dataService: DataService,
                                       val cacheService: CacheService,
                                       val logsService: AWSLogsService,
                                       implicit val actorSystem: ActorSystem,
                                       implicit val ec: ExecutionContext
                                       ) extends AWSLambdaService {

  import AWSLambdaConstants._

  val client: AWSLambdaAsync =
    AWSLambdaAsyncClientBuilder.standard().
      withRegion(region).
      withCredentials(credentialsProvider).
      build()

  val logsClient: AWSLogsAsync =
    AWSLogsAsyncClientBuilder.standard().
      withRegion(region).
      withCredentials(credentialsProvider).
      build()

  val apiBaseUrl: String = configuration.get[String](s"application.$API_BASE_URL_KEY")

  val invocationTimeoutSeconds: Int = configuration.get[Int]("aws.lambda.timeoutSeconds")

  val logSubscriptionsEnabled: Boolean = configuration.get[Boolean]("aws.logSubscriptions.enabled")
  val logSubscriptionsLambdaFunctionName: String = configuration.get[String]("aws.logSubscriptions.lambdaFunctionName")
  val logSubscriptionsFilterPattern: String = configuration.get[String]("aws.logSubscriptions.filterPattern")
  val logSubscriptionsFilterName: String = configuration.get[String]("aws.logSubscriptions.filterName")

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

  def listBehaviorGroupFunctionNames: Future[Seq[String]] = {
    fetchFunctions(None).map { functions =>
      val allFunctionNames = functions.map(_.getFunctionName)
      val behaviorFunctionNames = allFunctionNames.filter { ea =>
        ea.startsWith(BehaviorGroupVersion.lambdaFunctionPrefix)
      }
      behaviorFunctionNames
    }
  }

  def partionedBehaviorGroupFunctionNames: Future[PartitionedFunctionNames] = {
    for {
      allBehaviorGroupFunctionNames <- listBehaviorGroupFunctionNames
      currentFunctionNames <- dataService.behaviorGroupVersions.currentFunctionNames
    } yield {
      val missing = currentFunctionNames.diff(allBehaviorGroupFunctionNames)
      val current = currentFunctionNames.intersect(allBehaviorGroupFunctionNames)
      val obsolete = allBehaviorGroupFunctionNames.diff(currentFunctionNames)
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
                            behaviorVersion: BehaviorVersion,
                            token: InvocationToken,
                            payloadData: Seq[(String, JsValue)],
                            team: Team,
                            event: Event,
                            apiConfigInfo: ApiConfigInfo,
                            environmentVariables: Seq[EnvironmentVariable],
                            successFn: InvokeResult => BotResult,
                            maybeConversation: Option[Conversation],
                            isRetrying: Boolean,
                            defaultServices: DefaultServices
                          ): DBIO[BotResult] = {
    for {
      userInfo <- event.userInfoAction(defaultServices)
      result <- {
        DBIO.from(TeamInfo.forConfig(apiConfigInfo, userInfo, team, ws).flatMap { teamInfo =>
          val payloadJson = JsObject(
            payloadData ++ contextParamDataFor(environmentVariables, userInfo, teamInfo, token) ++ Seq(("behaviorVersionId", JsString(behaviorVersion.id)))
          )
          val invokeRequest =
            new InvokeRequest().
              withLogType(LogType.Tail).
              withFunctionName(behaviorVersion.groupVersion.functionName).
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
                    dataService.run(invokeFunctionAction(behaviorVersion, token, payloadData, team, event, apiConfigInfo, environmentVariables, successFn, maybeConversation, isRetrying=true, defaultServices))
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
                    maybeConversation: Option[Conversation],
                    defaultServices: DefaultServices
                  ): DBIO[BotResult] = {
    for {
      awsConfigs <- dataService.awsConfigs.allForAction(behaviorVersion.team)
      requiredAWSConfigs <- dataService.requiredAWSConfigs.allForAction(behaviorVersion.groupVersion)
      requiredOAuth2ApiConfigs <- dataService.requiredOAuth2ApiConfigs.allForAction(behaviorVersion.groupVersion)
      requiredSimpleTokenApis <- dataService.requiredSimpleTokenApis.allForAction(behaviorVersion.groupVersion)
      result <- if (behaviorVersion.functionBody.isEmpty) {
        DBIO.successful(SuccessResult(event, maybeConversation, JsNull, JsNull, parametersWithValues, behaviorVersion.maybeResponseTemplate, None, behaviorVersion.forcePrivateResponse))
      } else {
        for {
          user <- event.ensureUserAction(dataService)
          token <- dataService.invocationTokens.createForAction(user, behaviorVersion.behavior, event.maybeScheduled)
          invocationResult <- invokeFunctionAction(
            behaviorVersion,
            token,
            parametersWithValues.map { ea => (ea.invocationName, ea.preparedValue) },
            behaviorVersion.team,
            event,
            ApiConfigInfo(awsConfigs, requiredAWSConfigs, requiredOAuth2ApiConfigs, requiredSimpleTokenApis),
            environmentVariables,
            result => {
              val logString = new java.lang.String(new BASE64Decoder().decodeBuffer(result.getLogResult))
              val logResult = AWSLambdaLogResult.fromText(logString)
              behaviorVersion.resultFor(result.getPayload, logResult, parametersWithValues, dataService, configuration, event, maybeConversation)
            },
            maybeConversation,
            isRetrying = false,
            defaultServices
          )
        } yield invocationResult
      }
    } yield result
  }

  val amazonServiceExceptionRegex = """.*com\.amazonaws\.AmazonServiceException.*""".r
  val resourceNotFoundExceptionRegex = """com\.amazonaws\.services\.lambda\.model\.ResourceNotFoundException.*""".r

  val requireRegex = """.*require\s*\(['"]\s*(\S+)\s*['"]\).*""".r

  val alreadyIncludedModules = Array("aws-sdk", "dynamodb-doc")

  private def requiredModulesIn(code: String, libraries: Seq[LibraryVersion], includeLibraryRequires: Boolean): Seq[String] = {
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

  private def requiredModulesIn(behaviorVersions: Seq[BehaviorVersion], libraries: Seq[LibraryVersion], includeLibraryRequires: Boolean): Array[String] = {
    behaviorVersions.flatMap { ea =>
      requiredModulesIn(ea.functionBody, libraries, includeLibraryRequires = true)
    }.distinct.toArray
  }

  private def awsConfigCodeFor(required: RequiredAWSConfig): String = {
    if (required.isConfigured) {
      val teamInfoPath = s"event.$CONTEXT_PARAM.teamInfo.aws.${required.nameInCode}"
      s"""$CONTEXT_PARAM.aws.${required.nameInCode} = {
         |  accessKeyId: ${teamInfoPath}.accessKeyId,
         |  secretAccessKey: ${teamInfoPath}.secretAccessKey,
         |  region: ${teamInfoPath}.region,
         |};
         |
     """.stripMargin
    } else {
      ""
    }
  }

  private def awsCodeFor(apiConfigInfo: ApiConfigInfo): String = {
    if (apiConfigInfo.requiredAWSConfigs.isEmpty) {
      ""
    } else {
      s"""
         |$CONTEXT_PARAM.aws = {};
         |
         |${apiConfigInfo.requiredAWSConfigs.map(awsConfigCodeFor).mkString("\n")}
       """.stripMargin
    }
  }

  private def accessTokenCodeFor(app: RequiredOAuth2ApiConfig): String = {
    app.maybeApplication.map { application =>
      val infoKey =  if (application.api.grantType.requiresAuth) { "userInfo" } else { "teamInfo" }
      s"""$CONTEXT_PARAM.accessTokens.${app.nameInCode} = event.$CONTEXT_PARAM.$infoKey.links.find((ea) => ea.externalSystem === "${application.name}").token;"""
    }.getOrElse("")
  }

  private def accessTokensCodeFor(requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfig]): String = {
    requiredOAuth2ApiConfigs.map(accessTokenCodeFor).mkString("\n")
  }

  private def accessTokenCodeFor(required: RequiredSimpleTokenApi): String = {
    s"""$CONTEXT_PARAM.accessTokens.${required.nameInCode} = event.$CONTEXT_PARAM.userInfo.links.find((ea) => ea.externalSystem === "${required.api.name}").token;"""
  }

  private def simpleTokensCodeFor(requiredSimpleTokenApis: Seq[RequiredSimpleTokenApi]): String = {
    requiredSimpleTokenApis.map(accessTokenCodeFor).mkString("\n")
  }

  def functionWithParams(params: Array[String], functionBody: String): String = {
    s"""function(${(params ++ Array(CONTEXT_PARAM)).mkString(", ")}) {
        |  ${functionBody.trim}
        |}\n""".stripMargin
  }

  private def behaviorCodeFor(behaviorVersion: BehaviorVersion, params: Array[String]): String = {
    val paramsFromEvent = params.indices.map(i => s"event.${invocationParamFor(i)}")
    val invocationParamsString = (paramsFromEvent ++ Array(s"event.$CONTEXT_PARAM")).mkString(", ")
    s""""${behaviorVersion.id}": function() {
       |  var fn = require("./${behaviorVersion.jsName}");
       |  return fn($invocationParamsString);
       |}""".stripMargin
  }

  private def behaviorsCodeFor(behaviorVersionsWithParams: Seq[(BehaviorVersion, Array[String])]): String = {
    s"""var behaviors = {
       |  ${behaviorVersionsWithParams.map { case(bv, params) => behaviorCodeFor(bv, params)}.mkString(", ")}
       |}
     """.stripMargin
  }

  private def nodeCodeFor(
                           behaviorVersionsWithParams: Seq[(BehaviorVersion, Array[String])],
                           apiConfigInfo: ApiConfigInfo
                         ): String = {

    // Note: this attempts to make line numbers in the lambda script line up with those displayed in the UI
    // Be careful changing either this or the UI line numbers
    s"""exports.handler = function(event, context, lambdaCallback) { ${behaviorsCodeFor(behaviorVersionsWithParams)};
        |  const $CONTEXT_PARAM = event.$CONTEXT_PARAM;
        |
        |  $OVERRIDE_CONSOLE
        |  $CALLBACK_FUNCTION
        |  const callback = ellipsisCallback;
        |
        |  $NO_RESPONSE_CALLBACK_FUNCTION
        |  $SUCCESS_CALLBACK_FUNCTION
        |  $ERROR_CLASS
        |  $ERROR_CALLBACK_FUNCTION
        |
        |  $CONTEXT_PARAM.$NO_RESPONSE_KEY = ellipsisNoResponseCallback;
        |  $CONTEXT_PARAM.success = ellipsisSuccessCallback;
        |  $CONTEXT_PARAM.Error = EllipsisError;
        |  $CONTEXT_PARAM.error = ellipsisErrorCallback;
        |  process.removeAllListeners('unhandledRejection');
        |  process.on('unhandledRejection', $CONTEXT_PARAM.error);
        |
        |  ${awsCodeFor(apiConfigInfo)}
        |  $CONTEXT_PARAM.accessTokens = {};
        |  ${accessTokensCodeFor(apiConfigInfo.requiredOAuth2ApiConfigs)}
        |  ${simpleTokensCodeFor(apiConfigInfo.requiredSimpleTokenApis)}
        |
        |  try {
        |    behaviors[event.behaviorVersionId]();
        |  } catch(err) {
        |    $CONTEXT_PARAM.error(err);
        |  }
        |}
    """.stripMargin
  }

  private def dirNameFor(functionName: String) = s"/tmp/$functionName"
  private def zipFileNameFor(functionName: String) = s"${dirNameFor(functionName)}.zip"

  case class PreviousFunctionInfo(functionName: String, behaviorVersions: Seq[BehaviorVersion], libraries: Seq[LibraryVersion]) {
    val requiredModules = requiredModulesIn(behaviorVersions, libraries, includeLibraryRequires = true)
    val dirName = dirNameFor(functionName)
    val nodeModulesDirName = s"$dirName/node_modules"

    def canCopyModules(neededModules: Seq[String]): Boolean = {
      requiredModules.sameElements(neededModules) &&
        Files.exists(Paths.get(nodeModulesDirName))
    }

    def copyModulesInto(destinationDirName: String) = {
      Process(Seq("bash","-c",s"cp -r $nodeModulesDirName $destinationDirName/"), None, "HOME" -> "/tmp").!
    }
  }

  private def writeFileNamed(path: String, content: String) = {
    val writer = new PrintWriter(new File(path))
    writer.write(content)
    writer.close()
  }

  private def createZipWithModulesFor(
                                       functionName: String,
                                       behaviorVersionsWithParams: Seq[(BehaviorVersion, Array[String])],
                                       libraries: Seq[LibraryVersion],
                                       apiConfigInfo: ApiConfigInfo,
                                       maybePreviousFunctionInfo: Option[PreviousFunctionInfo],
                                       forceNodeModuleUpdate: Boolean
                                     ): Future[Unit] = {
    val dirName = dirNameFor(functionName)
    val path = Path(dirName)
    path.createDirectory()

    writeFileNamed(s"$dirName/index.js", nodeCodeFor(behaviorVersionsWithParams, apiConfigInfo))

    val behaviorVersionsDirName = s"$dirName/${BehaviorVersion.dirName}"
    Path(behaviorVersionsDirName).createDirectory()
    behaviorVersionsWithParams.foreach { case(behaviorVersion, params) =>
      writeFileNamed(s"$dirName/${behaviorVersion.jsName}", BehaviorVersion.codeFor(functionWithParams(params, behaviorVersion.functionBody)))
    }

    libraries.foreach { ea =>
      writeFileNamed(s"$dirName/${ea.jsName}", ea.code)
    }

    val requiredModules = requiredModulesIn(behaviorVersionsWithParams.map(_._1), libraries, includeLibraryRequires = true)
    val canCopyModules = maybePreviousFunctionInfo.exists { previousFunctionInfo =>
      if (previousFunctionInfo.canCopyModules(requiredModules)) {
        previousFunctionInfo.copyModulesInto(dirName)
        true
      } else {
        false
      }
    }
    for {
      _ <- if (forceNodeModuleUpdate || !canCopyModules) {
        Future.sequence(requiredModules.toSeq.map { moduleName =>
          // NPM wants to write a lockfile in $HOME; this makes it work for daemons
          Future {
            blocking(
              Process(Seq("bash", "-c", s"cd $dirName && npm install $moduleName"), None, "HOME" -> "/tmp").!
            )
          }
        })
      } else {
        Future.successful({})
      }
      _ <- Future {
        blocking(
          Process(Seq("bash","-c",s"cd $dirName && zip -q -r ${zipFileNameFor(functionName)} *")).!
        )
      }
    } yield {}

  }

  private def getNodeModuleInfoFor(functionName: String): JsValue = {
    val dirName = dirNameFor(functionName)
    val infoString = try {
      Process(Seq("bash", "-c", s"cd $dirName && npm list --depth=0 --json=true")).!!
    } catch {
      case t: Throwable => "{}"
    }
    Json.parse(infoString)
  }

  def ensureNodeModuleVersionsFor(groupVersion: BehaviorGroupVersion): DBIO[Seq[NodeModuleVersion]] = {
    val json = getNodeModuleInfoFor(groupVersion.functionName)
    val maybeDependencies = (json \ "dependencies").asOpt[JsObject]
    maybeDependencies.map { dependencies =>
      DBIO.sequence(dependencies.values.toSeq.map { depJson =>
        depJson.validate[NodeModuleVersionData] match {
          case JsSuccess(info, _) => {
            dataService.nodeModuleVersions.ensureForAction(info.from, info.version, groupVersion).map(Some(_))
          }
          case JsError(err) => DBIO.successful(None)
        }
      }).map(_.flatten)
    }.getOrElse(DBIO.successful(Seq()))
  }

  private def getZipFor(
                         functionName: String,
                         behaviorVersionsWithParams: Seq[(BehaviorVersion, Array[String])],
                         libraries: Seq[LibraryVersion],
                         apiConfigInfo: ApiConfigInfo,
                         maybePreviousFunctionInfo: Option[PreviousFunctionInfo],
                         forceNodeModuleUpdate: Boolean
                       ): Future[ByteBuffer] = {
    createZipWithModulesFor(
      functionName,
      behaviorVersionsWithParams,
      libraries,
      apiConfigInfo,
      maybePreviousFunctionInfo,
      forceNodeModuleUpdate
    ).map { _ =>
      val path = Paths.get(zipFileNameFor(functionName))
      ByteBuffer.wrap(Files.readAllBytes(path))
    }
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

  def logGroupNameFor(functionName: String): String = s"/aws/lambda/$functionName"

  def ensureLogGroupFor(functionName: String): Future[Any] = {
    val createLogGroupRequest =
      new CreateLogGroupRequest().withLogGroupName(logGroupNameFor(functionName))
    JavaFutureConverter.javaToScala(logsClient.createLogGroupAsync(createLogGroupRequest)).recover {
      case ex: ResourceConflictException => // no big deal if it's already created
    }
  }

  def setUpLogSubscriptionFor(functionName: String): Future[Any] = {
    if (logSubscriptionsEnabled) {
      ensureLogGroupFor(functionName).flatMap { _ =>
        val getFunctionRequest =
          new GetFunctionRequest().withFunctionName(logSubscriptionsLambdaFunctionName)
        JavaFutureConverter.javaToScala(client.getFunctionAsync(getFunctionRequest)).map { destinationFunctionResult =>
          val destinationFunctionArn: String = destinationFunctionResult.getConfiguration.getFunctionArn
          val request =
            new PutSubscriptionFilterRequest().
              withDestinationArn(destinationFunctionArn).
              withLogGroupName(logGroupNameFor(functionName)).
              withFilterName(logSubscriptionsFilterName).
              withFilterPattern(logSubscriptionsFilterPattern)
          logsClient.putSubscriptionFilter(request)
        }.recover {
          case t: Throwable => {
            Logger.error("Error trying to set up log subscription", t)
          }
        }
      }
    } else {
      Future.successful({})
    }
  }

  def deployFunction(
                      functionName: String,
                      behaviorVersionsWithParams: Seq[(BehaviorVersion, Array[String])],
                      libraries: Seq[LibraryVersion],
                      apiConfigInfo: ApiConfigInfo,
                      maybePreviousFunctionInfo: Option[PreviousFunctionInfo],
                      forceNodeModuleUpdate: Boolean
                    ): Future[Unit] = {

    val isNoCode: Boolean = behaviorVersionsWithParams.forall { case(bv, _) => bv.functionBody.trim.isEmpty }

    deleteFunction(functionName).andThen {
      case Failure(t) => Future.successful({})
      case Success(v) => if (isNoCode) {
        Future.successful(Unit)
      } else {
        for {
          functionCode <-getZipFor(
              functionName,
              behaviorVersionsWithParams,
              libraries,
              apiConfigInfo,
              maybePreviousFunctionInfo,
              forceNodeModuleUpdate
            ).map { zip => new FunctionCode().withZipFile(zip) }
          createFunctionRequest <- Future.successful(
            new CreateFunctionRequest().
              withFunctionName(functionName).
              withCode(functionCode).
              withRole(configuration.get[String]("aws.role")).
              withRuntime(com.amazonaws.services.lambda.model.Runtime.Nodejs610).
              withHandler("index.handler").
              withTimeout(invocationTimeoutSeconds)
          )
          _ <- JavaFutureConverter.javaToScala(client.createFunctionAsync(createFunctionRequest)).flatMap { result =>
            setUpLogSubscriptionFor(result.getFunctionName)
          }
        } yield {}
      }
    }
  }

  def deployFunctionFor(
                         groupVersion: BehaviorGroupVersion,
                         libraries: Seq[LibraryVersion],
                         behaviorVersionsWithParams: Seq[(BehaviorVersion, Array[String])],
                         apiConfigInfo: ApiConfigInfo,
                         forceNodeModuleUpdate: Boolean
                       ): Future[Unit] = {
    for {
      maybePrevious <- dataService.behaviorGroupVersions.maybePreviousFor(groupVersion)
      previousLibraries <- maybePrevious.map { prev =>
        dataService.libraries.allFor(prev)
      }.getOrElse(Future.successful(Seq()))
      previousBehaviorVersions <- maybePrevious.map { prev =>
        dataService.behaviorVersions.allForGroupVersion(prev)
      }.getOrElse(Future.successful(Seq()))
      maybePreviousFunctionInfo <- Future.successful(maybePrevious.map { version =>
        PreviousFunctionInfo(version.functionName, previousBehaviorVersions, previousLibraries)
      })
      _ <- deployFunction(
        groupVersion.functionName,
        behaviorVersionsWithParams,
        libraries,
        apiConfigInfo,
        maybePreviousFunctionInfo,
        forceNodeModuleUpdate
      )
    } yield {}
  }
}
