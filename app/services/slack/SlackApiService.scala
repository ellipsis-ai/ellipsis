package services.slack

import java.io.File

import _root_.models.accounts.slack.botprofile.SlackBotProfile
import akka.actor.ActorSystem
import akka.stream.scaladsl.{FileIO, Source}
import com.fasterxml.jackson.core.JsonParseException
import javax.inject.{Inject, Singleton}
import json.Formatting._
import json.SlackDialogParams
import json.slack.dialogs.SlackDialogInput
import models.behaviors.dialogs.Dialog
import models.behaviors.events.slack.SlackMessage
import play.api.Logger
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json._
import play.api.libs.ws.JsonBodyWritables._
import play.api.libs.ws.WSResponse
import play.api.mvc.MultipartFormData.{DataPart, FilePart}
import services.DefaultServices
import services.slack.apiModels._
import utils.SlackConversation

import scala.concurrent.{ExecutionContext, Future}

trait InvalidResponseException

case class TooManyRequestsException(ellipsisTeamId: String, slackTeamId: String) extends Exception(s"Slack API said too many requests for Slack team ${slackTeamId} (Ellipsis team ID ${ellipsisTeamId})") with InvalidResponseException
case class ErrorResponseException(status: Int, statusText: String) extends Exception(s"Slack API returned ${status}: ${statusText}") with InvalidResponseException
case class MalformedResponseException(message: String) extends Exception(message) with InvalidResponseException
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

  val slackEventService = services.slackEventService

  private def urlFor(method: String): String = s"$API_BASE_URL/$method"

  private def responseToJson(response: WSResponse, maybeField: Option[String] = None): JsValue = {
    if (response.status < 400) {
      try {
        response.json
      } catch {
        case j: JsonParseException => throw MalformedResponseException(
          s"""Slack API returned a non-JSON response${
            maybeField.map(field => s" while retrieving field ${field}").getOrElse(".")
          }
             |Ellipsis team ID: ${profile.teamId}
             |Slack team ID: ${profile.slackTeamId}
             |Error:
             |${j.getMessage}
             |
             |Truncated body:
             |${response.body.slice(0, 500)}
             |""".stripMargin
        )
      }
    } else if (response.status == 429) {
      Logger.error(s"""Slack API said too many requests to Slack API for team ID ${profile.teamId} with Slack team ID ${profile.slackTeamId}""")
      throw TooManyRequestsException(profile.slackTeamId, profile.teamId)
    } else {
      Logger.error(
        s"""Received irregular response from Slack API:
           |${response.status}: ${response.statusText}
           |
           |Truncated body:
           |${response.body.slice(0, 500)}
         """.stripMargin)
      throw ErrorResponseException(response.status, response.statusText)
    }
  }

  private def extract[T](response: WSResponse, field: String)(implicit fmt: Format[T]): T = {
    val json = responseToJson(response, Some(field))
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
      withHttpHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON).
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

  def permalinkFor(channel: String, messageTs: String): Future[Option[String]] = {
    val params = Seq(("channel", channel), ("message_ts", messageTs))
    getResponseFor("chat.getPermalink", params).
      map(r => Some(extract[String](r, "permalink"))).
      recover {
        case SlackApiError("message_not_found") => None // happens for simulated timestamps in RunEvents
        case SlackApiError("channel_not_found") => None // can happen when messages posted to user IDs
        case SlackApiError(err) => {
          Logger.error(
            s"""
               |Failed to retrieve permalink: $err
               |
               |Channel: $channel
               |Message timestamp: $messageTs
             """.stripMargin)
          None
        }
      }
  }

  case class SlackMessageJson(user: String, text: String, ts: String, thread_ts: Option[String])
  implicit val slackMessageFormat = Json.format[SlackMessageJson]

  def findReaction(channel: String, messageTs: String): Future[Option[SlackMessage]] = {
    val params = Seq(("channel", channel), ("timestamp", messageTs))
    getResponseFor("reactions.get", params).
      flatMap { r =>
        val msg = extract[SlackMessageJson](r, "message")
        SlackMessage.fromFormattedText(msg.text, profile, slackEventService, Some(messageTs), msg.thread_ts).map(Some(_))
      }.
      recover {
        case SlackApiError(err) => {
          Logger.error(
            s"""
               |Failed to retrieve reactions for message: $err
               |
               |Channel: $channel
               |Message timestamp: $messageTs
             """.stripMargin)
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
        val json = responseToJson(response, Some("channels"))
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

  def conversationMembers(convoId: String, maybeCursor: Option[String] = None): Future[Seq[String]] = {
    val params = Map("channel" -> convoId) ++ maybeCursor.map(c => Map("cursor" -> c)).getOrElse(Map())
    postResponseFor("conversations.members", params).
      flatMap { response =>
        val json = responseToJson(response, Some("members"))
        val batch = (json \ "members").asOpt[Seq[String]].getOrElse(Seq())
        val maybeNextCursor = (json \ "response_metadata" \ "next_cursor").asOpt[String].filter(_.trim.nonEmpty)
        maybeNextCursor.map { nextCursor =>
          conversationMembers(convoId, Some(nextCursor)).map(batch ++ _)
        }.getOrElse(Future.successful(batch))
      }
  }

  def openConversationFor(slackUserId: String): Future[String] = {
    postResponseFor("conversations.open", Map("users" -> slackUserId)).
      map { response =>
        val json = responseToJson(response, Some("channel.id"))
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

  def getTeamInfo: Future[Option[SlackTeam]] = {
    getResponseFor("team.info", Seq()).
      map(r => Some(extract[SlackTeam](r, "team"))).
      recover {
        case SlackApiError(err) => {
          Logger.error(
            s"""
               |Failed to retrieve team info: $err
               |
               |Team ID: ${profile.slackTeamId}
             """.stripMargin)
          None
        }
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
                  maybeFile: Option[File] = None,
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
    val endpoint = "files.upload"
    maybeFile.map { file =>
      val filePart = FilePart("file", filename.getOrElse("file"), filetype, FileIO.fromPath(file.toPath))
      val postParams = preparePostParams(params ++ defaultParams.toMap)
      val dataParts = postParams.map { case(k, v) => DataPart(k, v) }.toList
      val parts = filePart :: dataParts
      val url = urlFor(endpoint)
      ws.
        url(url).
        post(Source(parts))
    }.getOrElse {
      postResponseFor(endpoint, params)
    }.map { res =>
      extract[SlackFile](res, "file")
    }
  }

  def postChatMessage(channelId: String, text: String, username: Option[String] = None, asUser: Option[Boolean] = None,
                      parse: Option[String] = None, linkNames: Option[String] = None, attachments: Option[Seq[Attachment]] = None,
                      unfurlLinks: Option[Boolean] = None, unfurlMedia: Option[Boolean] = None, iconUrl: Option[String] = None,
                      iconEmoji: Option[String] = None, replaceOriginal: Option[Boolean]= None,
                      deleteOriginal: Option[Boolean] = None, threadTs: Option[String] = None, replyBroadcast: Option[Boolean] = None): Future[SlackMessage] = {

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
    postResponseFor("chat.postMessage", params).flatMap { r =>
      val msg = extract[SlackMessageJson](r, "message")
      SlackMessage.fromFormattedText(msg.text, profile, slackEventService, Some(msg.ts), msg.thread_ts)
    }
  }

  def postEphemeralMessage(
                            text: String,
                            channelId: String,
                            maybeThreadTs: Option[String],
                            userId: String,
                            asUser: Option[Boolean] = None,
                            parse: Option[String] = None,
                            linkNames: Option[String] = None,
                            attachments: Option[Seq[Attachment]] = None
                          ): Future[Unit] = {
    val params = Map(
      "channel" -> channelId,
      "thread_ts" -> maybeThreadTs,
      "text" -> text,
      "user" -> userId,
      "as_user" -> asUser,
      "parse" -> parse,
      "link_names" -> linkNames,
      "attachments" -> attachments.map(a => Json.stringify(Json.toJson(a)))
    )
    postResponseFor("chat.postEphemeral", params).map(_ => {})
  }

  def postToResponseUrl(
                         text: String,
                         maybeAttachments: Option[Seq[Attachment]],
                         responseUrl: String,
                         isEphemeral: Boolean,
                         replaceOriginal: Boolean = false
                       ): Future[Unit] = {
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
      map(_ => {})
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

  def allUsers(maybeCursor: Option[String] = None): Future[Seq[MembershipData]] = {
    val params = Map[String, Any](
    ) ++ maybeCursor.map(c => Seq(("cursor", c))).getOrElse(Seq())
    postResponseFor("users.list", params).flatMap { r =>
      val batch = extract[Seq[MembershipData]](r, "members")

      // Slack returns an empty string next_cursor rather than leaving it out
      val maybeNextCursor = (r.json \ "response_metadata" \ "next_cursor").asOpt[String].filter(_.trim.nonEmpty)
      maybeNextCursor.map { nextCursor =>
        allUsers(Some(nextCursor)).map(batch ++ _)
      }.getOrElse(Future.successful(batch))
    }
  }

  def accessLogs(before: Long): Future[JsValue] = {
    val params = Map(
      "before" -> before
    )
    ws.
      url(urlFor("team.accessLogs")).
      post(preparePostParams(params ++ defaultParams.toMap)).
      map { r =>
        extract[JsValue](r, "logins")
      }
  }

  case class ErrorMessages(messages: Seq[String])
  case class DialogOpenResponse(ok: Boolean, error: Option[String], response_metadata: Option[ErrorMessages])
  implicit val errorMessagesRead = Json.reads[ErrorMessages]
  implicit val dialogOpenResponse = Json.reads[DialogOpenResponse]

  def openDialog(dialog: Dialog, inputs: Seq[SlackDialogInput]) = {
    val params = Map(
      "dialog" -> Json.stringify(Json.toJson(SlackDialogParams(
        dialog.maybeTitle.getOrElse("ℹ️"),
        dialog.behaviorVersion.id,
        inputs,
        "Continue",
        notify_on_cancel = true,
        Json.stringify(Json.toJson(dialog.state))
      ))),
      "trigger_id" -> dialog.dialogInfo.triggerId
    )
    postResponseFor("dialog.open", params).map { r =>
      val json = responseToJson(r)
      val dialogResponse = json.as[DialogOpenResponse]
      if (!dialogResponse.ok) {
        Logger.error(
          s"""Error opening new Slack dialog: ${dialogResponse.error.getOrElse("(no error provided)")}
             |Error messages provided: ${dialogResponse.response_metadata.map(_.messages.mkString("\n- ")).getOrElse("(no messages provided)")}
             |""".stripMargin
        )
        dialogResponse.error.map(error => throw SlackApiError(error)).orElse {
          throw MalformedResponseException("Expected error message in non-ok response while opening dialog")
        }
      }
      dialogResponse.ok
    }
  }

}

@Singleton
class SlackApiService @Inject()(services: DefaultServices, implicit val actorSystem: ActorSystem, implicit val ec: ExecutionContext) {

  def clientFor(profile: SlackBotProfile): SlackApiClient = SlackApiClient(profile, services, actorSystem, ec)

  def adminClient: Future[SlackApiClient] = services.dataService.slackBotProfiles.admin.map(clientFor)

}
