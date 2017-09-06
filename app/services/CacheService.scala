package services

import javax.inject.{Inject, Provider, Singleton}

import json.BehaviorGroupData
import json.Formatting._
import models.accounts.slack.SlackUserInfo
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.behaviorparameter.ValidValue
import models.behaviors.events.{Event, SlackMessage, SlackMessageEvent}
import play.api.cache.CacheApi
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import slack.models.{Channel, Group, Im}

import scala.concurrent.duration._
import scala.reflect.ClassTag

case class SlackMessageEventData(
                                  profile: SlackBotProfile,
                                  channel: String,
                                  maybeThreadId: Option[String],
                                  user: String,
                                  message: SlackMessage,
                                  ts: String
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
        val eventData = SlackMessageEventData(ev.profile, ev.channel, ev.maybeThreadId, ev.user, ev.message, ev.ts)
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
            event.message,
            event.ts,
            slackEventService.clientFor(event.profile)
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

  private def slackChannelInfoKey(channel: String, teamId: String): String = {
    s"slack-team-$teamId-channel-$channel-info"
  }

  def cacheSlackChannelInfo(channel: String, teamId: String, data: Channel): Unit = {
    set(slackChannelInfoKey(channel, teamId), Json.toJson(data), 1.hour)
  }

  def uncacheSlackChannelInfo(channel: String, teamId: String): Unit = {
    remove(slackChannelInfoKey(channel, teamId))
  }

  def getSlackChannelInfo(channel: String, teamId: String): Option[Channel] = {
    get[JsValue](slackChannelInfoKey(channel, teamId)).flatMap { json =>
      json.validate[Channel] match {
        case JsSuccess(data, _) => Some(data)
        case JsError(_) => None
      }
    }
  }

  private def slackGroupInfoKey(group: String, teamId: String): String = {
    s"slack-team-$teamId-group-$group-info"
  }

  def cacheSlackGroupInfo(group: String, teamId: String, data: Group): Unit = {
    set(slackGroupInfoKey(group, teamId), Json.toJson(data), 1.hour)
  }

  def uncacheSlackGroupInfo(group: String, teamId: String): Unit = {
    remove(slackGroupInfoKey(group, teamId))
  }

  def getSlackGroupInfo(group: String, teamId: String): Option[Group] = {
    get[JsValue](slackGroupInfoKey(group, teamId)).flatMap { json =>
      json.validate[Group] match {
        case JsSuccess(data, _) => Some(data)
        case JsError(_) => None
      }
    }
  }

  private def slackImsKey(teamId: String): String = {
    s"slack-ims-team-$teamId"
  }

  def cacheSlackIMs(data: Seq[Im], teamId: String): Unit = {
    set(slackImsKey(teamId), Json.toJson(data), 10.seconds)
  }

  def getSlackIMs(teamId: String): Option[Seq[Im]] = {
    get[JsValue](slackImsKey(teamId)).flatMap { json =>
      json.validate[Seq[Im]] match {
        case JsSuccess(data, jsPath) => Some(data)
        case JsError(err) => None
      }
    }
  }

  def cacheBotUsername(userId: String, username: String): Unit = {
    set(s"slack-username-for-id-${userId}", username, 5.minutes)
  }

  def getBotUsername(userId: String): Option[String] = {
    get(s"slack-username-for-id-${userId}")
  }

}
