package services

import javax.inject.{Inject, Singleton}

import json.BehaviorGroupData
import json.Formatting._
import models.behaviors.behaviorparameter.ValidValue
import models.behaviors.events.{Event, SlackMessageEvent}
import play.api.cache.CacheApi
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}

import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

@Singleton
class CacheService @Inject() (
                               cache: CacheApi
                              ) {

  def set[T: ClassTag](key: String, value: T, expiration: Duration = Duration.Inf): Unit = {
    cache.set(key, value, expiration)
  }

  def get[T : ClassTag](key: String): Option[T] = {
    cache.get[T](key)
  }

  def remove(key: String) = {
    cache.remove(key)
  }

  def cacheEvent(key: String, event: Event, expiration: Duration = Duration.Inf): Unit = {
    event match {
      case ev: SlackMessageEvent => set(key, Json.toJson(ev), expiration)
      case _ =>
    }
  }

  def getEvent(key: String): Option[SlackMessageEvent] = {
    get[JsValue](key).flatMap { eventJson =>
      eventJson.validate[SlackMessageEvent] match {
        case JsSuccess(event, jsPath) => Some(event)
        case JsError(err) => None
      }
    }
  }

  def cacheValidValues(key: String, values: Seq[ValidValue], expiration: Duration = Duration.Inf): Unit = {
    set(key, Json.toJson(values), expiration)
  }

  def getValidValues(key: String): Option[Seq[ValidValue]] = {
    get[JsValue](key).flatMap { json =>
      json.validate[Seq[ValidValue]] match {
        case JsSuccess(values, jsPath) => Some(values)
        case JsError(err) => None
      }
    }
  }

  def cacheBehaviorGroupData(key: String, data: Seq[BehaviorGroupData], expiration: Duration = Duration.Inf): Unit = {
    set(key, Json.toJson(data), expiration)
  }

  def getBehaviorGroupData(key: String): Option[Seq[BehaviorGroupData]] = {
    get[JsValue](key).flatMap { json =>
      json.validate[Seq[BehaviorGroupData]] match {
        case JsSuccess(data, jsPath) => Some(data)
        case JsError(err) => None
      }
    }
  }

}
