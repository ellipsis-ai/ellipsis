package json

import models.accounts.User
import models.bots.triggers.MessageTriggerQueries
import models.bots.{BehaviorParameterQueries, BehaviorQueries}
import org.joda.time.DateTime
import play.api.libs.json.Json
import slick.dbio.DBIO
import scala.concurrent.ExecutionContext.Implicits.global


object EditorFormat {

  case class BehaviorParameterData(name: String, question: String)
  case class BehaviorTriggerData(
                                  text: String,
                                  requiresMention: Boolean,
                                  isRegex: Boolean,
                                  caseSensitive: Boolean
                                  )

  case class BehaviorVersionData(
                                  teamId: String,
                                  behaviorId: Option[String],
                                  functionBody: String,
                                  responseTemplate: String,
                                  params: Seq[BehaviorParameterData],
                                  triggers: Seq[BehaviorTriggerData],
                                  createdAt: Option[DateTime]
                                  )

  object BehaviorVersionData {

    private def extractFunctionBodyFrom(function: String): String = {
      """(?s)^\s*function\s*\([^\)]*\)\s*\{\s*(.*)\s*\}\s*$""".r.findFirstMatchIn(function).flatMap { m =>
        m.subgroups.headOption
      }.getOrElse("")
    }

    def fromStrings(teamId: String, function: String, response: String, params: String, triggers: String): BehaviorVersionData = {
      BehaviorVersionData(
        teamId,
        None,
        extractFunctionBodyFrom(function),
        response,
        Json.parse(params).validate[Seq[BehaviorParameterData]].get,
        Json.parse(triggers).validate[Seq[BehaviorTriggerData]].get,
        None
      )
    }

    def maybeFor(behaviorId: String, user: User): DBIO[Option[BehaviorVersionData]] = {
      for {
        maybeBehavior <- BehaviorQueries.find(behaviorId, user)
        maybeBehaviorVersion <- maybeBehavior.map { behavior =>
          behavior.maybeCurrentVersion
        }.getOrElse(DBIO.successful(None))
        maybeParameters <- maybeBehaviorVersion.map { behaviorVersion =>
          BehaviorParameterQueries.allFor(behaviorVersion).map(Some(_))
        }.getOrElse(DBIO.successful(None))
        maybeTriggers <- maybeBehaviorVersion.map { behaviorVersion =>
          MessageTriggerQueries.allFor(behaviorVersion).map(Some(_))
        }.getOrElse(DBIO.successful(None))
      } yield {
        for {
          behavior <- maybeBehavior
          behaviorVersion <- maybeBehaviorVersion
          params <- maybeParameters
          triggers <- maybeTriggers
        } yield {
          BehaviorVersionData(
            behaviorVersion.team.id,
            Some(behavior.id),
            behaviorVersion.functionBody,
            behaviorVersion.maybeResponseTemplate.getOrElse(""),
            params.map { ea =>
              BehaviorParameterData(ea.name, ea.question)
            },
            triggers.map(ea =>
              BehaviorTriggerData(ea.pattern, requiresMention = ea.requiresBotMention, isRegex = ea.shouldTreatAsRegex, caseSensitive = ea.isCaseSensitive)
            ),
            Some(behaviorVersion.createdAt)
          )
        }
      }
    }
  }

  case class BehaviorData(behaviorId: String, versions: Seq[BehaviorVersionData])

  implicit val behaviorParameterReads = Json.reads[BehaviorParameterData]
  implicit val behaviorParameterWrites = Json.writes[BehaviorParameterData]

  implicit val behaviorTriggerReads = Json.reads[BehaviorTriggerData]
  implicit val behaviorTriggerWrites = Json.writes[BehaviorTriggerData]

  implicit val behaviorVersionReads = Json.reads[BehaviorVersionData]
  implicit val behaviorVersionWrites = Json.writes[BehaviorVersionData]

  implicit val behaviorReads = Json.reads[BehaviorData]
  implicit val behaviorWrites = Json.writes[BehaviorData]

}
