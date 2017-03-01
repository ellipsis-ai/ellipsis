package json

case class BehaviorConfig(
                           exportId: Option[String],
                           name: Option[String],
                           aws: Option[AWSConfigData],
                           requiredOAuth2ApiConfigs: Option[Seq[RequiredOAuth2ApiConfigData]],
                           requiredSimpleTokenApis: Option[Seq[RequiredSimpleTokenApiData]],
                           forcePrivateResponse: Option[Boolean],
                           dataTypeName: Option[String]
                           ) {
  val knownEnvVarsUsed: Seq[String] = {
    aws.map(_.knownEnvVarsUsed).getOrElse(Seq())
  }

  def copyForExport: BehaviorConfig = {
    // we don't want to export the team-specific application, but we want to keep the scope
    val requiredOAuth2ApiConfigsForExport = requiredOAuth2ApiConfigs.map { configs =>
      configs.map(_.copyForExport)
    }
    val requiredSimpleTokenApisForExport = requiredSimpleTokenApis.map { configs =>
      configs.map(_.copyForExport)
    }
    copy(requiredOAuth2ApiConfigs = requiredOAuth2ApiConfigsForExport, requiredSimpleTokenApis = requiredSimpleTokenApisForExport)
  }
}
