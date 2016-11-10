package models.behaviors.config.requiredsimpletokenapi

import json.RequiredSimpleTokenApiData
import models.accounts.simpletokenapi.SimpleTokenApi
import models.behaviors.behaviorversion.BehaviorVersion

import scala.concurrent.Future

trait RequiredSimpleTokenApiService {

  def allFor(behaviorVersion: BehaviorVersion): Future[Seq[RequiredSimpleTokenApi]]

  def allFor(api: SimpleTokenApi, behaviorVersion: BehaviorVersion): Future[Seq[RequiredSimpleTokenApi]]

  def find(id: String): Future[Option[RequiredSimpleTokenApi]]

  def save(requiredOAuth2ApiConfig: RequiredSimpleTokenApi): Future[RequiredSimpleTokenApi]

  def maybeCreateFor(data: RequiredSimpleTokenApiData, behaviorVersion: BehaviorVersion): Future[Option[RequiredSimpleTokenApi]]

}
