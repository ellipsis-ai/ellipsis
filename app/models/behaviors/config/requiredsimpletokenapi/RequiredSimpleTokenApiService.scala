package models.behaviors.config.requiredsimpletokenapi

import json.RequiredSimpleTokenApiData
import models.accounts.simpletokenapi.SimpleTokenApi
import models.accounts.user.User
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import slick.dbio.DBIO

import scala.concurrent.Future

trait RequiredSimpleTokenApiService {

  def allForAction(groupVersion: BehaviorGroupVersion): DBIO[Seq[RequiredSimpleTokenApi]]

  def allFor(groupVersion: BehaviorGroupVersion): Future[Seq[RequiredSimpleTokenApi]]

  def allFor(api: SimpleTokenApi, groupVersion: BehaviorGroupVersion): Future[Seq[RequiredSimpleTokenApi]]

  def missingFor(user: User, groupVersion: BehaviorGroupVersion): Future[Seq[RequiredSimpleTokenApi]]

  def find(id: String): Future[Option[RequiredSimpleTokenApi]]

  def save(requiredOAuth2ApiConfig: RequiredSimpleTokenApi): Future[RequiredSimpleTokenApi]

  def maybeCreateForAction(data: RequiredSimpleTokenApiData, groupVersion: BehaviorGroupVersion): DBIO[Option[RequiredSimpleTokenApi]]

}
