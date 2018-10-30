package controllers.api.json

case class GenerateApiTokenInfo(
                                 token: String,
                                 expirySeconds: Option[Int],
                                 isOneTime: Option[Boolean]
                               ) extends ApiMethodInfo
