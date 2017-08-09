package services

import javax.inject.{Inject, Provider, Singleton}

import json.BehaviorGroupData
import json.Formatting._
import models.accounts.slack.SlackUserInfo
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.behaviorparameter.ValidValue
import models.behaviors.events.{Event, SlackMessageEvent}
import play.api.cache.CacheApi
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}

import scala.concurrent.duration._
import scala.reflect.ClassTag

case class SlackMessageEventData(
                                  profile: SlackBotProfile,
                                  channel: String,
                                  maybeThreadId: Option[String],
                                  user: String,
                                  text: String,
                                  ts: String,
                                  slackUserList: Seq[SlackUserInfo]
                                )

@Singleton
class CacheService @Inject() (
                               cache: CacheApi,
                               slackEventServiceProvider: Provider[SlackEventService]
                              ) {

  def slackEventService = slackEventServiceProvider.get

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
      case ev: SlackMessageEvent => {
        val eventData = SlackMessageEventData(ev.profile, ev.channel, ev.maybeThreadId, ev.user, ev.text, ev.ts, ev.slackUserList)
        set(key, Json.toJson(eventData), expiration)
      }
      case _ =>
    }
  }

  def getEvent(key: String): Option[SlackMessageEvent] = {
    get[JsValue](key).flatMap { eventJson =>
      eventJson.validate[SlackMessageEventData] match {
        case JsSuccess(event, jsPath) => {
          Some(SlackMessageEvent(
            event.profile,
            event.channel,
            event.maybeThreadId,
            event.user,
            event.text,
            event.ts,
            slackEventService.clientFor(event.profile),
            event.slackUserList
          ))
        }
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

  def cacheSlackUserList(key: String, data: Seq[SlackUserInfo]): Unit = {
    // TODO: we should probably make this last longer, and invalidate it based on events we receive from Slack
    set(key, Json.toJson(data), 1.minute)
  }

  def getSlackUserList(key: String): Option[Seq[SlackUserInfo]] = {
    get[JsValue](key).flatMap { json =>
      json.validate[Seq[SlackUserInfo]] match {
        case JsSuccess(data, jsPath) => Some(data)
        case JsError(err) => None
      }
    }
  }

}
