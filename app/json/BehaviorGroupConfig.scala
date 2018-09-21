package json

case class BehaviorGroupConfig(
                                name: String,
                                exportId: Option[String],
                                icon: Option[String],
                                requiredAWSConfigs: Option[Seq[RequiredAWSConfigData]],
                                requiredOAuthApiConfigs: Option[Seq[RequiredOAuthApiConfigData]],
                                requiredSimpleTokenApis: Option[Seq[RequiredSimpleTokenApiData]]
                              ) {

  def copyForExport: BehaviorGroupConfig = {
    // we don't want to export the team-specific application, but we want to keep the scope
    val requiredAWSConfigsForExport = requiredAWSConfigs.map(_.map(_.copyForExport))
    val requiredOAuthApiConfigsForExport = requiredOAuthApiConfigs.map(_.map(_.copyForExport))
    val requiredSimpleTokenApisForExport = requiredSimpleTokenApis.map(_.map(_.copyForExport))
    copy(
      requiredAWSConfigs = requiredAWSConfigsForExport,
      requiredOAuthApiConfigs = requiredOAuthApiConfigsForExport,
      requiredSimpleTokenApis = requiredSimpleTokenApisForExport
    )
  }

}
