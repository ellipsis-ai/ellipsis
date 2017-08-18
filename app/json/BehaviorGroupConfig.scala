package json

case class BehaviorGroupConfig(
                                name: String,
                                exportId: Option[String],
                                icon: Option[String],
                                awsConfig: Option[AWSConfigData],
                                requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfigData],
                                requiredSimpleTokenApis: Seq[RequiredSimpleTokenApiData]
                              ) {

  def copyForExport: BehaviorGroupConfig = {
    // we don't want to export the team-specific application, but we want to keep the scope
    val requiredOAuth2ApiConfigsForExport = requiredOAuth2ApiConfigs.map(_.copyForExport)
    val requiredSimpleTokenApisForExport = requiredSimpleTokenApis.map(_.copyForExport)
    copy(requiredOAuth2ApiConfigs = requiredOAuth2ApiConfigsForExport, requiredSimpleTokenApis = requiredSimpleTokenApisForExport)
  }

}
