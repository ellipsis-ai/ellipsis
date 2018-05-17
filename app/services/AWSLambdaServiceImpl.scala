package services

import java.io.{File, PrintWriter}
import java.nio.ByteBuffer
import java.nio.file.{Files, Paths}
import java.security.MessageDigest
import java.time.OffsetDateTime

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

  private def contextParamDataFor(
                                   environmentVariables: Seq[EnvironmentVariable],
                                   userInfo: UserInfo,
                                   teamInfo: TeamInfo,
                                   eventInfo: EventInfo,
                                   token: InvocationToken
                                   ): Seq[(String, JsObject)] = {
    val teamEnvVars = environmentVariables.filter(ev => ev.isInstanceOf[TeamEnvironmentVariable])
    Seq(CONTEXT_PARAM -> JsObject(Seq(
      API_BASE_URL_KEY -> JsString(apiBaseUrl),
      TOKEN_KEY -> JsString(token.id),
      ENV_KEY -> JsObject(teamEnvVars.map { ea =>
        ea.name -> JsString(ea.value)
      }),
      USER_INFO_KEY -> userInfo.toJson,
      TEAM_INFO_KEY -> teamInfo.toJson,
      EVENT_INFO_KEY -> eventInfo.toJson
    )))
  }

  private def cacheKeyFor(behaviorVersion: BehaviorVersion, payloadData: Seq[(String, JsValue)]): String = {
    val payloadBytes: Array[Byte] = Json.toJson(payloadData).toString.getBytes("UTF-8")
    val md: MessageDigest = MessageDigest.getInstance("MD5")
    val digest: Array[Byte] = md.digest(payloadBytes)
    s"lambda-${behaviorVersion.id}-${digest}"
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
                            retryIntervals: List[Long],
                            defaultServices: DefaultServices
                          ): DBIO[BotResult] = for {
                            userInfo <- event.userInfoAction(defaultServices)
                            result <- {
                              DBIO.from(TeamInfo.forConfig(apiConfigInfo, userInfo, team, ws).flatMap { teamInfo =>
                                val payloadJson = JsObject(
                                  payloadData ++ contextParamDataFor(environmentVariables, userInfo, teamInfo, EventInfo(event), token) ++ Seq(("behaviorVersionId", JsString(behaviorVersion.id)))
                                )
                                val cacheKey = cacheKeyFor(behaviorVersion, payloadData)
                                println(cacheKey)
                                defaultServices.cacheService.getInvokeResult(cacheKey).map(Future.successful).getOrElse {
                                  println(s"running ${behaviorVersion.id}")
                                  val invokeRequest =
                                    new InvokeRequest().
                                      withLogType(LogType.Tail).
                                      withFunctionName(behaviorVersion.groupVersion.functionName).
                                      withInvocationType(InvocationType.RequestResponse).
                                      withPayload(payloadJson.toString())
                                  JavaFutureConverter.javaToScala(client.invokeAsync(invokeRequest)).map { res =>
                                    if (behaviorVersion.canBeMemoized && res.getFunctionError == null) {
                                      defaultServices.cacheService.cacheInvokeResult(cacheKey, res)
                                    }
                                    res
                                  }
                                }.map(successFn).recoverWith {
                                  case e: java.util.concurrent.ExecutionException => {
                                    e.getMessage match {
                                      case amazonServiceExceptionRegex() => Future.successful(AWSDownResult(event, behaviorVersion, maybeConversation))
                                      case resourceNotFoundExceptionRegex() => {
                                        retryIntervals.headOption.map { retryInterval =>
                                          Logger.info(s"retrying behavior invocation after resource not found with interval: ${retryInterval}s")
                                          Thread.sleep(retryInterval*1000)
                                          dataService.run(invokeFunctionAction(behaviorVersion, token, payloadData, team, event, apiConfigInfo, environmentVariables, successFn, maybeConversation, retryIntervals.tail, defaultServices))
                                        }.getOrElse {
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
      developerContext <- DeveloperContext.buildFor(event, behaviorVersion, dataService)
      result <- if (behaviorVersion.functionBody.isEmpty) {
        DBIO.successful(
          SuccessResult(
            event,
            behaviorVersion,
            maybeConversation,
            JsNull,
            JsNull,
            parametersWithValues,
            behaviorVersion.maybeResponseTemplate,
            None,
            behaviorVersion.forcePrivateResponse,
            developerContext
          )
        )
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
              behaviorVersion.resultFor(
                result.getPayload,
                logResult,
                parametersWithValues,
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
        } yield invocationResult
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
          _ <- JavaFutureConverter.javaToScala(client.createFunctionAsync(createFunctionRequest)).flatMap { result =>
            setUpLogSubscriptionFor(result.getFunctionName)
          }
        } yield {}
      }
    }
  }

}
