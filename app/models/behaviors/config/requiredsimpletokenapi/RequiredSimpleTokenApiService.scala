package models.behaviors.config.requiredsimpletokenapi

import json.RequiredSimpleTokenApiData
import models.accounts.simpletokenapi.SimpleTokenApi
import models.accounts.user.User
import models.behaviors.behaviorversion.BehaviorVersion
import slick.dbio.DBIO

import scala.concurrent.Future

trait RequiredSimpleTokenApiService {

  def allFor(behaviorVersion: BehaviorVersion): Future[Seq[RequiredSimpleTokenApi]]

  def allFor(api: SimpleTokenApi, behaviorVersion: BehaviorVersion): Future[Seq[RequiredSimpleTokenApi]]

  def missingFor(user: User, behaviorVersion: BehaviorVersion): Future[Seq[RequiredSimpleTokenApi]]

  def find(id: String): Future[Option[RequiredSimpleTokenApi]]

  def save(requiredOAuth2ApiConfig: RequiredSimpleTokenApi): Future[RequiredSimpleTokenApi]

  def maybeCreateForAction(data: RequiredSimpleTokenApiData, behaviorVersion: BehaviorVersion): DBIO[Option[RequiredSimpleTokenApi]]

}
