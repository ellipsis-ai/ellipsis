package services.slack

import java.io.File

import _root_.models.accounts.slack.botprofile.SlackBotProfile
import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import json.Formatting._
import play.api.Logger
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import services.DefaultServices
import services.slack.apiModels._
import utils.{SlackConversation, SlackTimestamp}

import scala.concurrent.{ExecutionContext, Future}

case class MalformedResponseException(message: String) extends Exception(message)
case class SlackApiError(code: String) extends Exception(code)


case class SlackApiClient(
                           profile: SlackBotProfile,
                           services: DefaultServices,
                           implicit val actorSystem: ActorSystem,
                           implicit val ec: ExecutionContext
                         ) {

  import Formatting._

  val token: String = profile.token

  private val SLACK_CONVERSATIONS_BATCH_SIZE = 1000

  private val API_BASE_URL = "https://slack.com/api/"
  private val ws = services.ws

  private def urlFor(method: String): String = s"$API_BASE_URL/$method"

  private def extract[T](response: WSResponse, field: String)(implicit fmt: Format[T]): T = {
    val json = response.json
    (json \ field).validate[T] match {
      case JsSuccess(v, _) => v
      case JsError(_) => {
        (json \ "error").validate[String] match {
          case JsSuccess(code, _) => throw SlackApiError(code)
          case JsError(errors) => throw MalformedResponseException(
            s"""Error converting Slack API data in field `${field}`.
               |Ellipsis team ID: ${profile.teamId}
               |Slack team ID: ${profile.slackTeamId}
               |JSON error:
               |${JsError.toJson(errors).toString()}
               """.stripMargin
          )
        }

      }
    }
  }

  private def preparePostParams(params: Map[String,Any]): Map[String,String] = {
    params.flatMap {
      case (k, Some(v)) => Some(k -> v.toString)
      case (k, None) => None
      case (k, v) => Some(k -> v.toString)
    }
  }

  val defaultParams: Seq[(String, String)] = Seq(("token", profile.token))

  private def postResponseFor(endpoint: String, params: Map[String, Any]): Future[WSResponse] = {
    ws.
      url(urlFor(endpoint)).
      withHttpHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON).
      post(preparePostParams(params ++ defaultParams.toMap))
  }

  private def getResponseFor(endpoint: String, params: Seq[(String, String)]): Future[WSResponse] = {
    Logger.info(s"SlackApiClient query $endpoint with params $params")
    ws.
      url(urlFor(endpoint)).
      withQueryStringParameters((params ++ defaultParams): _*).
      get
  }

  def conversationInfo(convoId: String): Future[Option[SlackConversation]] = {
    val params = Map("channel" -> convoId)
    postResponseFor("conversations.info", params).
      map(r => Some(extract[SlackConversation](r, "channel"))).
      recover {
        case SlackApiError("channel_not_found") => None
      }
  }

  def permaLinkFor(channel: String, messageTs: String): Future[Option[String]] = {
    val params = Seq(("channel", channel), ("message_ts", messageTs))
    getResponseFor("chat.getPermalink", params).
      map(r => Some(extract[String](r, "permalink"))).
      recover {
        case SlackApiError(err) => {
          Logger.error(s"Failed to retrieve permalink: $err")
          None
        }
      }
  }

  def listConversations(maybeCursor: Option[String] = None): Future[Seq[SlackConversation]] = {
    val params = Seq(
      ("types", "public_channel, private_channel, mpim, im"),
      ("exclude_archived", "false"),
      ("limit", SLACK_CONVERSATIONS_BATCH_SIZE.toString)
    ) ++ maybeCursor.map(c => Seq(("cursor", c))).getOrElse(Seq())
    getResponseFor("conversations.list", params).
      flatMap { response =>
        val json = response.json
        val batch = (json \ "channels").validate[Seq[SlackConversation]] match {
          case JsSuccess(data, _) => data
          case JsError(err) => {
            Logger.error(s"Failed to parse SlackConversation from conversations.list: ${JsError.toJson(err).toString}")
            Seq()
          }
        }
        // Slack returns an empty string next_cursor rather than leaving it out
        val maybeNextCursor = (json \ "response_metadata" \ "next_cursor").asOpt[String].filter(_.trim.nonEmpty)
        maybeNextCursor.map { nextCursor =>
          listConversations(Some(nextCursor)).map(batch ++ _)
        }.getOrElse(Future.successful(batch))
      }
  }

  def conversationMembers(convoId: String): Future[Seq[String]] = {
    postResponseFor("conversations.members", Map("channel" -> convoId)).
      map { response =>
        (response.json \ "members").asOpt[Seq[String]].getOrElse(Seq())
      }
  }

  def openConversationFor(slackUserId: String): Future[String] = {
    postResponseFor("conversations.open", Map("users" -> slackUserId)).
      map { response =>
        val json = response.json
        if ((json \ "ok").as[Boolean]) {
          (json \ "channel" \ "id").validate[String] match {
            case JsSuccess(id, _) => id
            case JsError(err) => throw MalformedResponseException(err.toString)
          }
        } else {
          throw SlackApiError((json \ "error").as[String])
        }
      }
  }

  def getUserInfo(slackUserId: String): Future[Option[SlackUser]] = {
    postResponseFor("users.info", Map("user" -> slackUserId)).map { r =>
      Some(extract[SlackUser](r, "user"))
    }.recover {
      case SlackApiError(code) if code == "user_not_found" || code == "account_inactive" => None
    }
  }

  def getUserInfoByEmail(email: String): Future[Option[SlackUser]] = {
    postResponseFor("users.lookupByEmail", Map("email" -> email)).map { r =>
      Some(extract[SlackUser](r, "user"))
    }.recover {
      case SlackApiError(code) if code == "users_not_found" => None
    }
  }

  def uploadFile(
                  file: File,
                  content: Option[String] = None,
                  filetype: Option[String] = None,
                  filename: Option[String] = None,
                  title: Option[String] = None,
                  initialComment: Option[String] = None,
                  channels: Option[Seq[String]] = None,
                  maybeThreadTs: Option[String] = None
                ): Future[SlackFile] = {
    val params = Map(
      "content" -> content,
      "filetype" -> filetype,
      "filename" -> filename,
      "title" -> title,
      "initial_comment" -> initialComment,
      "channels" -> channels.map(_.mkString(",")),
      "thread_ts" -> maybeThreadTs
    )
    postResponseFor("files.upload", params).map { res =>
      extract[SlackFile](res, "file")
    }
  }

  def postChatMessage(channelId: String, text: String, username: Option[String] = None, asUser: Option[Boolean] = None,
                      parse: Option[String] = None, linkNames: Option[String] = None, attachments: Option[Seq[Attachment]] = None,
                      unfurlLinks: Option[Boolean] = None, unfurlMedia: Option[Boolean] = None, iconUrl: Option[String] = None,
                      iconEmoji: Option[String] = None, replaceOriginal: Option[Boolean]= None,
                      deleteOriginal: Option[Boolean] = None, threadTs: Option[String] = None, replyBroadcast: Option[Boolean] = None): Future[String] = {

    val params = Map(
      "channel" -> channelId,
      "text" -> text,
      "username" -> username,
      "as_user" -> asUser,
      "parse" -> parse,
      "link_names" -> linkNames,
      "attachments" -> attachments.map(a => Json.stringify(Json.toJson(a))),
      "unfurl_links" -> unfurlLinks,
      "unfurl_media" -> unfurlMedia,
      "icon_url" -> iconUrl,
      "icon_emoji" -> iconEmoji,
      "replace_original" -> replaceOriginal,
      "delete_original" -> deleteOriginal,
      "delete_original" -> deleteOriginal,
      "thread_ts" -> threadTs,
      "reply_broadcast" -> replyBroadcast
    )
    postResponseFor("chat.postMessage", params).map { r =>
      extract[String](r, "ts")
    }
  }

  def postEphemeralMessage(text: String, channelId: String, userId: String, asUser: Option[Boolean] = None,
                           parse: Option[String] = None, linkNames: Option[String] = None, attachments: Option[Seq[Attachment]] = None): Future[String] = {
    val params = Map(
      "channel" -> channelId,
      "text" -> text,
      "user" -> userId,
      "as_user" -> asUser,
      "parse" -> parse,
      "link_names" -> linkNames,
      "attachments" -> attachments.map(a => Json.stringify(Json.toJson(a)))
    )
    postResponseFor("chat.postEphemeral", params).map { r =>
      extract[String](r, "message_ts")
    }
  }

  def postToResponseUrl(
                         text: String,
                         maybeAttachments: Option[Seq[Attachment]],
                         responseUrl: String,
                         isEphemeral: Boolean,
                         replaceOriginal: Boolean = false
                       ): Future[String] = {
    val responseType = if (isEphemeral) { "ephemeral" } else { "in_channel" }
    val payload = Json.obj(
      "response_type" -> JsString(responseType),
      "replace_original" -> Json.toJson(replaceOriginal),
      "text" -> JsString(text)
    ) ++ maybeAttachments.map { attachments =>
      Json.obj(
        "attachments" -> attachments.map(a => Json.toJson(a))
      )
    }.getOrElse(Json.obj())
    services.ws.
      url(responseUrl).
      withHttpHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON).
      post(payload).
      map { r =>
        // These endpoints seem to just return a 200 OK with no data, so let's simulate a timestamp
        SlackTimestamp.now
      }
  }

  def addReactionToMessage(emojiName: String, channelId: String, timestamp: String): Future[Boolean] = {
    val params = Map(
      "name" -> emojiName,
      "channel" -> channelId,
      "timestamp" -> timestamp
    )
    postResponseFor("reactions.add", params).map { r =>
      extract[Boolean](r, "ok")
    }
  }

  def removeReaction(emojiName: String, file: Option[String] = None, fileComment: Option[String] = None, channelId: Option[String] = None,
                     timestamp: Option[String] = None): Future[Boolean] = {
    val params = Map(
      "name" -> emojiName,
      "file" -> file,
      "file_comment" -> fileComment,
      "channel" -> channelId,
      "timestamp" -> timestamp
    )
    postResponseFor("reactions.remove", params).map { r =>
      extract[Boolean](r, "ok")
    }
  }

  def removeReactionFromMessage(emojiName: String, channelId: String, timestamp: String): Future[Boolean] = {
    removeReaction(emojiName = emojiName, channelId = Some(channelId), timestamp = Some(timestamp))
  }

}

@Singleton
class SlackApiService @Inject()(services: DefaultServices, implicit val actorSystem: ActorSystem, implicit val ec: ExecutionContext) {

  def clientFor(profile: SlackBotProfile): SlackApiClient = SlackApiClient(profile, services, actorSystem, ec)

  def adminClient: Future[SlackApiClient] = services.dataService.slackBotProfiles.admin.map(clientFor)

}
