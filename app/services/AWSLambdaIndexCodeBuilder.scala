package services

import models.behaviors.behaviorparameter.BehaviorParameter
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.config.requiredawsconfig.RequiredAWSConfig
import models.behaviors.config.requiredoauth1apiconfig.RequiredOAuth1ApiConfig
import models.behaviors.config.requiredoauth2apiconfig.RequiredOAuth2ApiConfig
import models.behaviors.config.requiredsimpletokenapi.RequiredSimpleTokenApi
import services.AWSLambdaConstants._

case class AWSLambdaIndexCodeBuilder(
                                 behaviorVersionsWithParams: Seq[(BehaviorVersion, Seq[BehaviorParameter])],
                                 apiConfigInfo: ApiConfigInfo
                               ) {

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

  private def oauth1AccessTokenCodeFor(app: RequiredOAuth1ApiConfig): String = {
    app.maybeApplication.map { application =>
      s"""$CONTEXT_PARAM.accessTokens.${app.nameInCode} = event.$CONTEXT_PARAM.userInfo.links.find((ea) => ea.externalSystem === "${application.name}").token;"""
    }.getOrElse("")
  }

  private def oauth1AccessTokensCodeFor(requiredOAuth1ApiConfigs: Seq[RequiredOAuth1ApiConfig]): String = {
    requiredOAuth1ApiConfigs.map(oauth1AccessTokenCodeFor).mkString("\n")
  }

  private def oauth2AccessTokenCodeFor(app: RequiredOAuth2ApiConfig): String = {
    app.maybeApplication.map { application =>
      val infoKey =  if (application.api.grantType.requiresAuth) { "userInfo" } else { "teamInfo" }
      s"""$CONTEXT_PARAM.accessTokens.${app.nameInCode} = event.$CONTEXT_PARAM.$infoKey.links.find((ea) => ea.externalSystem === "${application.name}").token;"""
    }.getOrElse("")
  }

  private def oauth2AccessTokensCodeFor(requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfig]): String = {
    requiredOAuth2ApiConfigs.map(oauth2AccessTokenCodeFor).mkString("\n")
  }

  private def accessTokenCodeFor(required: RequiredSimpleTokenApi): String = {
    s"""$CONTEXT_PARAM.accessTokens.${required.nameInCode} = event.$CONTEXT_PARAM.userInfo.links.find((ea) => ea.externalSystem === "${required.api.name}").token;"""
  }

  private def simpleTokensCodeFor(requiredSimpleTokenApis: Seq[RequiredSimpleTokenApi]): String = {
    requiredSimpleTokenApis.map(accessTokenCodeFor).mkString("\n")
  }

  private def behaviorMappingFor(behaviorVersion: BehaviorVersion, params: Seq[BehaviorParameter]): String = {
    val paramsFromEvent = params.indices.map(i => s"event.${invocationParamFor(i)}")
    val invocationParamsString = (paramsFromEvent ++ Array(s"event.$CONTEXT_PARAM")).mkString(", ")
    s""""${behaviorVersion.id}": function() {
       |  var fn = require("./${behaviorVersion.jsName}");
       |  return fn($invocationParamsString);
       |}""".stripMargin
  }

  private def behaviorsMap: String = {
    s"""var behaviors = {
       |  ${behaviorVersionsWithParams.map { case(bv, params) => behaviorMappingFor(bv, params)}.mkString(", ")}
       |}
     """.stripMargin
  }

  def build: String = {
    s"""exports.handler = function(event, context, lambdaCallback) {
       |  $behaviorsMap;
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
       |  $ELLIPSIS_REQUIRE_FUNCTION
       |  $ADD_UPLOAD_FUNCTIONS
       |
       |  $CONTEXT_PARAM.$NO_RESPONSE_KEY = ellipsisNoResponseCallback;
       |  $CONTEXT_PARAM.success = ellipsisSuccessCallback;
       |  $CONTEXT_PARAM.Error = EllipsisError;
       |  $CONTEXT_PARAM.error = ellipsisErrorCallback;
       |  $CONTEXT_PARAM.require = ellipsisRequire;
       |  addUploadFunctionsTo($CONTEXT_PARAM);
       |  process.removeAllListeners('unhandledRejection');
       |  process.on('unhandledRejection', $CONTEXT_PARAM.error);
       |  process.removeAllListeners('uncaughtException');
       |  process.on('uncaughtException', $CONTEXT_PARAM.error);
       |
       |  ${awsCodeFor(apiConfigInfo)}
       |  $CONTEXT_PARAM.accessTokens = {};
       |  ${oauth1AccessTokensCodeFor(apiConfigInfo.requiredOAuth1ApiConfigs)}
       |  ${oauth2AccessTokensCodeFor(apiConfigInfo.requiredOAuth2ApiConfigs)}
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

}
