package services

import java.io.{File, PrintWriter}
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.time.OffsetDateTime
import java.util.Base64

import akka.actor.ActorSystem
import com.amazonaws.services.lambda.model._
import com.amazonaws.services.lambda.{AWSLambdaAsync, AWSLambdaAsyncClientBuilder}
import com.amazonaws.services.logs.model.{CreateLogGroupRequest, PutSubscriptionFilterRequest}
import com.amazonaws.services.logs.{AWSLogsAsync, AWSLogsAsyncClientBuilder}
import com.fasterxml.jackson.core.JsonParseException
import javax.inject.Inject
import models.behaviors._
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorparameter.{BehaviorParameter, FileType}
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.config.requiredawsconfig.RequiredAWSConfig
import models.behaviors.config.requiredoauth2apiconfig.RequiredOAuth2ApiConfig
import models.behaviors.config.requiredsimpletokenapi.RequiredSimpleTokenApi
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import models.behaviors.invocationtoken.InvocationToken
import models.behaviors.library.LibraryVersion
import models.behaviors.nodemoduleversion.NodeModuleVersion
import models.environmentvariable.{EnvironmentVariable, TeamEnvironmentVariable}
import models.team.Team
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}
import services.caching.CacheService
import slick.dbio.DBIO
import sun.misc.BASE64Decoder
import utils.{JavaFutureConverter, RequiredModulesInCode}

import scala.collection.JavaConversions.asScalaBuffer
import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.io.Source
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

  val logSubscriptionsEnabled: Boolean = configuration.get[Boolean]("aws.logSubscriptions.enabled")
  val logSubscriptionsLambdaFunctionName: String = configuration.get[String]("aws.logSubscriptions.lambdaFunctionName")
  val logSubscriptionsFilterPattern: String = configuration.get[String]("aws.logSubscriptions.filterPattern")
  val logSubscriptionsFilterName: String = configuration.get[String]("aws.logSubscriptions.filterName")
  val initialInvocationRetrySeconds: Int = configuration.get[Int]("aws.lambda.initialInvocationRetrySeconds")
  val numInvocationRetries: Int = configuration.get[Int]("aws.lambda.numInvocationRetries")
  val invocationRetryIntervals: List[Long] = 0.until(numInvocationRetries).map { i =>
    initialInvocationRetrySeconds * scala.math.pow(2, i).toLong
  }.toList

  def createdFileNameFor(groupVersion: BehaviorGroupVersion): String = {
    dirNameFor(groupVersion.functionName) ++ "/created"
  }

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

  def partitionedBehaviorGroupFunctionNames: Future[PartitionedFunctionNames] = {
    for {
      allBehaviorGroupFunctionNames <- listBehaviorGroupFunctionNames
      activeFunctionNames <- dataService.behaviorGroupVersions.activeFunctionNames
    } yield {
      val missing = activeFunctionNames.diff(allBehaviorGroupFunctionNames)
      val current = activeFunctionNames.intersect(allBehaviorGroupFunctionNames)
      val obsolete = allBehaviorGroupFunctionNames.diff(activeFunctionNames)
      PartitionedFunctionNames(current, missing, obsolete)
    }
  }

  private def teamInfoFor(behaviorVersion: BehaviorVersion, userInfo: UserInfo, botInfo: BotInfo): DBIO[TeamInfo] = {
    val team = behaviorVersion.team
    val groupVersion = behaviorVersion.groupVersion
    for {
      awsConfigs <- dataService.awsConfigs.allForAction(team)
      requiredAWSConfigs <- dataService.requiredAWSConfigs.allForAction(groupVersion)
      requiredOAuth2ApiConfigs <- dataService.requiredOAuth2ApiConfigs.allForAction(groupVersion)
      requiredSimpleTokenApis <- dataService.requiredSimpleTokenApis.allForAction(groupVersion)
      teamInfo <- DBIO.from {
        val apiConfigInfo = ApiConfigInfo(awsConfigs, requiredAWSConfigs, requiredOAuth2ApiConfigs, requiredSimpleTokenApis)
        TeamInfo.forConfig(apiConfigInfo, userInfo, team, botInfo, ws)
      }
    } yield teamInfo
  }

  private def invocationJsonFor(
                                 behaviorVersion: BehaviorVersion,
                                 userInfo: UserInfo,
                                 teamInfo: TeamInfo,
                                 eventInfo: EventInfo,
                                 parameterValues: Seq[ParameterWithValue],
                                 environmentVariables: Seq[EnvironmentVariable],
                                 token: InvocationToken
                               ): JsObject = {
    val parameterValueData = parameterValues.map { ea => (ea.invocationName, ea.preparedValue) }
    val teamEnvVars = environmentVariables.filter(ev => ev.isInstanceOf[TeamEnvironmentVariable])
    val contextParamData = Seq(
      CONTEXT_PARAM -> JsObject(Seq(
        API_BASE_URL_KEY -> JsString(apiBaseUrl),
        TOKEN_KEY -> JsString(token.id),
        ENV_KEY -> JsObject(teamEnvVars.map { ea =>
          ea.name -> JsString(ea.value)
        }),
        USER_INFO_KEY -> userInfo.toJson,
        TEAM_INFO_KEY -> teamInfo.toJson,
        EVENT_INFO_KEY -> eventInfo.toJson
      ))
    )
    JsObject(parameterValueData ++ contextParamData ++ Seq(("behaviorVersionId", JsString(behaviorVersion.id))))
  }

  private def cacheKeyFor(behaviorVersion: BehaviorVersion, payloadData: Seq[(String, JsValue)]): String = {
    val payloadString = Base64.getEncoder.encodeToString(Json.toJson(payloadData).toString.getBytes(StandardCharsets.UTF_8))
    s"lambda-${behaviorVersion.id}-${payloadString}"
  }

  private def maybeCachedResultFor(
                                    behaviorVersion: BehaviorVersion,
                                    payloadData: Seq[(String, JsValue)]
                                  ): Option[InvokeResult] = {
    if (behaviorVersion.canBeMemoized) {
      val cacheKey = cacheKeyFor(behaviorVersion, payloadData)
      cacheService.getInvokeResult(cacheKey)
    } else {
      None
    }
  }

  def invokeFunctionAction(
                            behaviorVersion: BehaviorVersion,
                            token: InvocationToken,
                            payloadData: Seq[(String, JsValue)],
                            invocationJson: JsObject,
                            event: Event,
                            successFn: InvokeResult => BotResult,
                            maybeConversation: Option[Conversation],
                            retryIntervals: List[Long],
                            defaultServices: DefaultServices
                          ): DBIO[BotResult] = {
    DBIO.from {
      maybeCachedResultFor(behaviorVersion, payloadData).map(Future.successful).getOrElse {
        Logger.info(s"running lambda function for ${behaviorVersion.id}")
        val invokeRequest =
          new InvokeRequest().
            withLogType(LogType.Tail).
            withFunctionName(behaviorVersion.groupVersion.functionName).
            withInvocationType(InvocationType.RequestResponse).
            withPayload(invocationJson.toString())
        JavaFutureConverter.javaToScala(client.invokeAsync(invokeRequest)).map { res =>
          if (behaviorVersion.canBeMemoized && res.getFunctionError == null) {
            cacheService.cacheInvokeResult(cacheKeyFor(behaviorVersion, payloadData), res)
          }
          res
        }
      }.map(successFn).recoverWith {
        case e: java.util.concurrent.ExecutionException => {
          e.getMessage match {
            case amazonServiceExceptionRegex() => {
              Future.successful(AWSDownResult(event, behaviorVersion, maybeConversation, dataService))
            }
            case resourceNotFoundExceptionRegex() => {
              retryIntervals.headOption.map { retryInterval =>
                Logger.info(s"retrying behavior invocation after resource not found with interval: ${retryInterval}s")
                Thread.sleep(retryInterval * 1000)
                dataService.run(invokeFunctionAction(
                  behaviorVersion,
                  token,
                  payloadData,
                  invocationJson,
                  event,
                  successFn,
                  maybeConversation,
                  retryIntervals.tail,
                  defaultServices
                ))
              }.getOrElse {
                throw e
              }
            }
            case _ => throw e
          }
        }
      }
    }
  }

  def invokeAction(
                    behaviorVersion: BehaviorVersion,
                    parametersWithValues: Seq[ParameterWithValue],
                    environmentVariables: Seq[EnvironmentVariable],
                    event: Event,
                    maybeConversation: Option[Conversation],
                    defaultServices: DefaultServices
                  )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult] = {
    for {
      developerContext <- DeveloperContext.buildFor(event, behaviorVersion, dataService)
      userInfo <- event.userInfoAction(maybeConversation, defaultServices)
      botName <- DBIO.from(event.botName(defaultServices))
      token <- dataService.invocationTokens.createForAction(userInfo.user, behaviorVersion, event.maybeScheduled)
      teamInfo <- teamInfoFor(behaviorVersion, userInfo, BotInfo(botName, event.botUserIdForContext))
      result <- {
        val invocationJson = invocationJsonFor(
          behaviorVersion,
          userInfo,
          teamInfo,
          EventInfo(event),
          parametersWithValues,
          environmentVariables,
          token
        )
        if (behaviorVersion.functionBody.isEmpty) {
          DBIO.successful(SuccessResult(
            event,
            behaviorVersion,
            maybeConversation,
            JsNull,
            JsNull,
            parametersWithValues,
            invocationJson,
            behaviorVersion.maybeResponseTemplate,
            None,
            behaviorVersion.forcePrivateResponse,
            developerContext
          ))
        } else {
          invokeFunctionAction(
            behaviorVersion,
            token,
            parametersWithValues.map { ea => (ea.invocationName, ea.preparedValue) },
            invocationJson,
            event,
            result => {
              val logString = new java.lang.String(new BASE64Decoder().decodeBuffer(result.getLogResult))
              val logResult = AWSLambdaLogResult.fromText(logString)
              behaviorVersion.resultFor(
                result.getPayload,
                logResult,
                parametersWithValues,
                invocationJson,
                dataService,
                configuration,
                event,
                maybeConversation,
                developerContext
              )
            },
            maybeConversation,
            invocationRetryIntervals,
            defaultServices
          )
        }
      }
    } yield result
  }

  val amazonServiceExceptionRegex = """.*com\.amazonaws\.AmazonServiceException.*""".r
  val resourceNotFoundExceptionRegex = """com\.amazonaws\.services\.lambda\.model\.ResourceNotFoundException.*""".r

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

  def decorateParams(params: Seq[BehaviorParameter]): String = {
    params.map { ea =>
      ea.input.paramType.decorationCodeFor(ea)
    }.mkString("")
  }

  def functionWithParams(params: Seq[BehaviorParameter], functionBody: String, isForExport: Boolean): String = {
    val paramNames = params.map(_.input.name)
    val paramDecoration = if (isForExport) { "" } else { decorateParams(params) }
    s"""function(${(paramNames ++ Array(CONTEXT_PARAM)).mkString(", ")}) {
        |  $paramDecoration${functionBody.trim}
        |}\n""".stripMargin
  }

  private def behaviorMappingFor(behaviorVersion: BehaviorVersion, params: Seq[BehaviorParameter]): String = {
    val paramsFromEvent = params.indices.map(i => s"event.${invocationParamFor(i)}")
    val invocationParamsString = (paramsFromEvent ++ Array(s"event.$CONTEXT_PARAM")).mkString(", ")
    s""""${behaviorVersion.id}": function() {
       |  var fn = require("./${behaviorVersion.jsName}");
       |  return fn($invocationParamsString);
       |}""".stripMargin
  }

  private def behaviorsMapFor(behaviorVersionsWithParams: Seq[(BehaviorVersion, Seq[BehaviorParameter])]): String = {
    s"""var behaviors = {
       |  ${behaviorVersionsWithParams.map { case(bv, params) => behaviorMappingFor(bv, params)}.mkString(", ")}
       |}
     """.stripMargin
  }

  private def hasFileParams(behaviorVersionsWithParams: Seq[(BehaviorVersion, Seq[BehaviorParameter])]): Boolean = {
    behaviorVersionsWithParams.exists { case(_, params) => params.exists(_.input.paramType == FileType) }
  }

  private def nodeCodeFor(
                           behaviorVersionsWithParams: Seq[(BehaviorVersion, Seq[BehaviorParameter])],
                           apiConfigInfo: ApiConfigInfo
                         ): String = {
    s"""exports.handler = function(event, context, lambdaCallback) {
        |  ${behaviorsMapFor(behaviorVersionsWithParams)};
        |
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
        |  $CONTEXT_PARAM.require = function(module) { return require(module.replace(/@.+$$/, "")); }
        |  process.removeAllListeners('unhandledRejection');
        |  process.on('unhandledRejection', $CONTEXT_PARAM.error);
        |  process.removeAllListeners('uncaughtException');
        |  process.on('uncaughtException', $CONTEXT_PARAM.error);
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

  private def writeFileNamed(path: String, content: String) = {
    val writer = new PrintWriter(new File(path))
    writer.write(content)
    writer.close()
  }

  private def requiredModulesForFileParams(behaviorVersionsWithParams: Seq[(BehaviorVersion, Seq[BehaviorParameter])]): Seq[String] = {
    if (hasFileParams(behaviorVersionsWithParams)) {
      Seq("request")
    } else {
      Seq()
    }
  }

  private def createZipWithModulesFor(
                                       functionName: String,
                                       behaviorVersionsWithParams: Seq[(BehaviorVersion, Seq[BehaviorParameter])],
                                       libraries: Seq[LibraryVersion],
                                       apiConfigInfo: ApiConfigInfo
                                     ): Future[Unit] = {
    val dirName = dirNameFor(functionName)
    val path = Path(dirName)
    path.createDirectory()

    writeFileNamed(s"$dirName/index.js", nodeCodeFor(behaviorVersionsWithParams, apiConfigInfo))

    val behaviorVersionsDirName = s"$dirName/${BehaviorVersion.dirName}"
    Path(behaviorVersionsDirName).createDirectory()
    behaviorVersionsWithParams.foreach { case(behaviorVersion, params) =>
      writeFileNamed(s"$dirName/${behaviorVersion.jsName}", BehaviorVersion.codeFor(functionWithParams(params, behaviorVersion.functionBody, isForExport = false)))
    }

    if (hasFileParams(behaviorVersionsWithParams)) {
      writeFileNamed(s"$dirName/$FETCH_FUNCTION_FOR_FILE_PARAM_NAME.js", FETCH_FUNCTION_FOR_FILE_PARAM)
    }

    libraries.foreach { ea =>
      writeFileNamed(s"$dirName/${ea.jsName}", ea.code)
    }

    val requiredModulesForBehaviorVersions = RequiredModulesInCode.requiredModulesIn(behaviorVersionsWithParams.map(_._1), libraries, includeLibraryRequires = true)
    val requiredModules = (requiredModulesForBehaviorVersions ++ requiredModulesForFileParams(behaviorVersionsWithParams)).distinct
    for {
      _ <- if (requiredModules.isEmpty) {
        Future.successful({})
      } else {
        Future {
          blocking(
            Process(Seq("bash", "-c", s"cd $dirName && npm init -f && npm install ${requiredModules.mkString(" ")}"), None, "HOME" -> "/tmp").!
          )
        }
      }
      _ <- Future {
        blocking(
          Process(Seq("bash","-c",s"cd $dirName && zip -q -r ${zipFileNameFor(functionName)} *")).!
        )
      }
    } yield {}

  }

  private def getNodeModuleInfoFor(groupVersion: BehaviorGroupVersion): JsValue = {
    val dirName = dirNameFor(groupVersion.functionName)
    val timeout = OffsetDateTime.now.plusSeconds(10)
    while (timeout.isAfter(OffsetDateTime.now) && !Path(createdFileNameFor(groupVersion)).exists) {
      Thread.sleep(1000)
    }
    val packageName = s"$dirName/package.json"
    if (Path(packageName).exists) {
      try {
        Json.parse(Source.fromFile(packageName).getLines.mkString)
      } catch {
        case _: JsonParseException => JsObject.empty
      }
    } else {
      JsObject.empty
    }
  }

  def hasNodeModuleVersions(groupVersion: BehaviorGroupVersion): DBIO[Boolean] = {
    for {
      behaviorVersions <- dataService.behaviorVersions.allForGroupVersionAction(groupVersion)
      libraries <- dataService.libraries.allForAction(groupVersion)
    } yield {
      behaviorVersions.exists(_.hasFunction) && RequiredModulesInCode.requiredModulesIn(behaviorVersions, libraries, includeLibraryRequires = true).nonEmpty
    }
  }

  def ensureNodeModuleVersionsFor(groupVersion: BehaviorGroupVersion): DBIO[Seq[NodeModuleVersion]] = {
    for {
      hasNodeModuleVersions <- hasNodeModuleVersions(groupVersion)
      nodeModuleVersions <- if (hasNodeModuleVersions) {
        dataService.nodeModuleVersions.allForAction(groupVersion).flatMap { existing =>
          if (existing.isEmpty) {
            val json = getNodeModuleInfoFor(groupVersion)
            val maybeDependencies = (json \ "dependencies").asOpt[JsObject]
            maybeDependencies.map { dependencies =>
              DBIO.sequence(dependencies.value.toSeq.map { case (name, version) =>
                dataService.nodeModuleVersions.ensureForAction(name, version.as[String], groupVersion)
              }).map(_.sortBy(_.nameWithoutVersion))
            }.getOrElse(DBIO.successful(Seq()))
          } else {
            DBIO.successful(existing)
          }
        }

      } else {
        DBIO.successful(Seq())
      }
    } yield nodeModuleVersions
  }

  private def getZipFor(
                         functionName: String,
                         behaviorVersionsWithParams: Seq[(BehaviorVersion, Seq[BehaviorParameter])],
                         libraries: Seq[LibraryVersion],
                         apiConfigInfo: ApiConfigInfo
                       ): Future[ByteBuffer] = {
    createZipWithModulesFor(
      functionName,
      behaviorVersionsWithParams,
      libraries,
      apiConfigInfo
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

  def deployFunctionFor(
                         groupVersion: BehaviorGroupVersion,
                         behaviorVersionsWithParams: Seq[(BehaviorVersion, Seq[BehaviorParameter])],
                         libraries: Seq[LibraryVersion],
                         apiConfigInfo: ApiConfigInfo
                    ): Future[Unit] = {

    val isNoCode: Boolean = behaviorVersionsWithParams.forall { case(bv, _) => bv.functionBody.trim.isEmpty }
    val functionName = groupVersion.functionName

    deleteFunction(functionName).andThen {
      case Failure(t) => Future.successful({})
      case Success(v) => if (isNoCode) {
        Future.successful(Unit)
      } else {
        for {
          functionCode <- getZipFor(
              functionName,
              behaviorVersionsWithParams,
              libraries,
              apiConfigInfo
            ).map { zip => new FunctionCode().withZipFile(zip) }
          _ <- Future.successful(writeFileNamed(createdFileNameFor(groupVersion), OffsetDateTime.now.toString))
          createFunctionRequest <- Future.successful(
            new CreateFunctionRequest().
              withFunctionName(functionName).
              withCode(functionCode).
              withRole(configuration.get[String]("aws.role")).
              withRuntime(com.amazonaws.services.lambda.model.Runtime.Nodejs610).
              withHandler("index.handler").
              withTimeout(invocationTimeoutSeconds)
          )
          result <- JavaFutureConverter.javaToScala(client.createFunctionAsync(createFunctionRequest))
          _ <- setUpLogSubscriptionFor(result.getFunctionName)
        } yield {}
      }
    }
  }

}
