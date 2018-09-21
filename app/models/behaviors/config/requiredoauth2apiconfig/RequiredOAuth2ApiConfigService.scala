package models.behaviors.config.requiredoauth2apiconfig

import json.RequiredOAuthApiConfigData
import models.accounts.oauth2api.OAuth2Api
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import slick.dbio.DBIO

import scala.concurrent.Future

trait RequiredOAuth2ApiConfigService {

  def allForAction(groupVersion: BehaviorGroupVersion): DBIO[Seq[RequiredOAuth2ApiConfig]]

  def allForId(groupVersionId: String): Future[Seq[RequiredOAuth2ApiConfig]]

  def allFor(groupVersion: BehaviorGroupVersion): Future[Seq[RequiredOAuth2ApiConfig]]

  def allFor(api: OAuth2Api, behaviorGroup: BehaviorGroup): Future[Seq[RequiredOAuth2ApiConfig]]

  def allFor(api: OAuth2Api, groupVersion: BehaviorGroupVersion): Future[Seq[RequiredOAuth2ApiConfig]]

  def find(id: String): Future[Option[RequiredOAuth2ApiConfig]]

  def findWithNameInCode(nameInCode: String, groupVersion: BehaviorGroupVersion): Future[Option[RequiredOAuth2ApiConfig]]

  def save(requiredOAuth2ApiConfig: RequiredOAuth2ApiConfig): Future[RequiredOAuth2ApiConfig]

  def maybeCreateForAction(data: RequiredOAuthApiConfigData, groupVersion: BehaviorGroupVersion): DBIO[Option[RequiredOAuth2ApiConfig]]

  def maybeCreateFor(data: RequiredOAuthApiConfigData, groupVersion: BehaviorGroupVersion): Future[Option[RequiredOAuth2ApiConfig]]

}
