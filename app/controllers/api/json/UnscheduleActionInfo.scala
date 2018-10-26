package controllers.api.json

case class UnscheduleActionInfo(
                                 actionName: Option[String],
                                 trigger: Option[String],
                                 userId: Option[String],
                                 channel: Option[String],
                                 token: String
                               ) extends ApiMethodWithActionInfo
