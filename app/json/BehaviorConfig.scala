package json

case class BehaviorConfig(
                           exportId: Option[String],
                           name: Option[String],
                           aws: Option[AWSConfigData],
                           forcePrivateResponse: Option[Boolean],
                           isDataType: Boolean,
                           dataTypeConfig: Option[DataTypeConfigData]
                           ) {

  val knownEnvVarsUsed: Seq[String] = {
    aws.map(_.knownEnvVarsUsed).getOrElse(Seq())
  }

  def copyForClone: BehaviorConfig = {
    copy(
      dataTypeConfig = dataTypeConfig.map(_.copyForClone)
    )
  }

}
