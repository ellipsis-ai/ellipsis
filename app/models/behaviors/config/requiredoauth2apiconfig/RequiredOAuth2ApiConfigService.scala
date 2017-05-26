package models.behaviors.config.requiredoauth2apiconfig

import json.RequiredOAuth2ApiConfigData
import models.accounts.oauth2api.OAuth2Api
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import slick.dbio.DBIO

import scala.concurrent.Future

trait RequiredOAuth2ApiConfigService {

  def allFor(groupVersion: BehaviorGroupVersion): Future[Seq[RequiredOAuth2ApiConfig]]

  def allFor(api: OAuth2Api, behaviorGroup: BehaviorGroup): Future[Seq[RequiredOAuth2ApiConfig]]

  def allFor(api: OAuth2Api, groupVersion: BehaviorGroupVersion): Future[Seq[RequiredOAuth2ApiConfig]]

  def find(id: String): Future[Option[RequiredOAuth2ApiConfig]]

  def save(requiredOAuth2ApiConfig: RequiredOAuth2ApiConfig): Future[RequiredOAuth2ApiConfig]

  def maybeCreateForAction(data: RequiredOAuth2ApiConfigData, groupVersion: BehaviorGroupVersion): DBIO[Option[RequiredOAuth2ApiConfig]]

  def maybeCreateFor(data: RequiredOAuth2ApiConfigData, groupVersion: BehaviorGroupVersion): Future[Option[RequiredOAuth2ApiConfig]]

}
