package json

case class BehaviorGroupConfig(
                                name: String,
                                exportId: Option[String],
                                icon: Option[String],
                                requiredAWSConfigs: Seq[RequiredAWSConfigData],
                                requiredOAuth1ApiConfigs: Seq[RequiredOAuth1ApiConfigData],
                                requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfigData],
                                requiredSimpleTokenApis: Seq[RequiredSimpleTokenApiData]
                              ) {

  def copyForExport: BehaviorGroupConfig = {
    // we don't want to export the team-specific application, but we want to keep the scope
    val requiredAWSConfigsForExport = requiredAWSConfigs.map(_.copyForExport)
    val requiredOAuth1ApiConfigsForExport = requiredOAuth1ApiConfigs.map(_.copyForExport)
    val requiredOAuth2ApiConfigsForExport = requiredOAuth2ApiConfigs.map(_.copyForExport)
    val requiredSimpleTokenApisForExport = requiredSimpleTokenApis.map(_.copyForExport)
    copy(
      requiredAWSConfigs = requiredAWSConfigsForExport,
      requiredOAuth1ApiConfigs = requiredOAuth1ApiConfigsForExport,
      requiredOAuth2ApiConfigs = requiredOAuth2ApiConfigsForExport,
      requiredSimpleTokenApis = requiredSimpleTokenApisForExport
    )
  }

}
