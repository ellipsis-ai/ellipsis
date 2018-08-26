package models.behaviors.config.requiredoauth1apiconfig

import json.RequiredOAuthApiConfigData
import models.accounts.oauth1api.OAuth1Api
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import slick.dbio.DBIO

import scala.concurrent.Future

trait RequiredOAuth1ApiConfigService {

  def allForAction(groupVersion: BehaviorGroupVersion): DBIO[Seq[RequiredOAuth1ApiConfig]]

  def allForId(groupVersionId: String): Future[Seq[RequiredOAuth1ApiConfig]]

  def allFor(groupVersion: BehaviorGroupVersion): Future[Seq[RequiredOAuth1ApiConfig]]

  def allFor(api: OAuth1Api, behaviorGroup: BehaviorGroup): Future[Seq[RequiredOAuth1ApiConfig]]

  def allFor(api: OAuth1Api, groupVersion: BehaviorGroupVersion): Future[Seq[RequiredOAuth1ApiConfig]]

  def find(id: String): Future[Option[RequiredOAuth1ApiConfig]]

  def findWithNameInCode(nameInCode: String, groupVersion: BehaviorGroupVersion): Future[Option[RequiredOAuth1ApiConfig]]

  def save(requiredOAuth2ApiConfig: RequiredOAuth1ApiConfig): Future[RequiredOAuth1ApiConfig]

  def maybeCreateForAction(data: RequiredOAuthApiConfigData, groupVersion: BehaviorGroupVersion): DBIO[Option[RequiredOAuth1ApiConfig]]

  def maybeCreateFor(data: RequiredOAuthApiConfigData, groupVersion: BehaviorGroupVersion): Future[Option[RequiredOAuth1ApiConfig]]

}
