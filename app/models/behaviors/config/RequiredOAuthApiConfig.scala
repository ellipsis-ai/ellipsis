package models.behaviors.config

import models.accounts.{OAuthApi, OAuthApplication}
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion

trait RequiredOAuthApiConfig {
  val id: String
  val exportId: String
  val groupVersion: BehaviorGroupVersion
  val api: OAuthApi
  val maybeRecommendedScope: Option[String]
  val nameInCode: String
  val maybeApplication: Option[OAuthApplication]
}
