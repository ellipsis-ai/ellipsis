package models.bots

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util
import com.github.tototoshi.slick.PostgresJodaSupport._
import models.accounts.User
import models.bots.templates.{SlackRenderer, TemplateApplier}
import models.{EnvironmentVariableQueries, IDs, Team}
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.node.{Image, AbstractVisitor, Node}
import org.commonmark.parser.Parser
import org.joda.time.DateTime
import play.api.libs.json.{JsString, JsDefined, Json, JsValue}
import play.api.{Configuration, Play}
import services.AWSLambdaConstants._
import services.AWSLambdaService
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CommonmarkVisitor extends AbstractVisitor {
  override def visit(image: Image) {
    image.unlink()
  }
}

case class BehaviorVersion(
                     id: String,
                     team: Team,
                     maybeDescription: Option[String],
                     maybeShortName: Option[String],
                     maybeFunctionBody: Option[String],
                     maybeResponseTemplate: Option[String],
                     createdAt: DateTime
                     ) {

  def isSkill: Boolean = {
    maybeFunctionBody.map { body =>
      Option(body).filter(_.trim.nonEmpty).isDefined
    }.getOrElse(false)
  }

  def editLinkFor(configuration: Configuration): Option[String] = {
    configuration.getString("application.apiBaseUrl").map { baseUrl =>
      val path = controllers.routes.ApplicationController.editBehavior(id)
      s"$baseUrl$path"
    }
  }

  def description: String = maybeDescription.getOrElse("")

  def functionBody: String = maybeFunctionBody.getOrElse("")

  lazy val conf = Play.current.configuration

  def functionName: String = id

  def resultFor(parametersWithValues: Seq[ParameterWithValue], service: AWSLambdaService): Future[String] = {
    for {
      envVars <- service.models.run(EnvironmentVariableQueries.allFor(team))
      result <- service.invoke(this, parametersWithValues, envVars)
    } yield result
  }

  def unlearn(lambdaService: AWSLambdaService): DBIO[Unit] = {
    lambdaService.deleteFunction(id)
    BehaviorVersionQueries.delete(this).map(_ => Unit)
  }

  def learnCode(code: String, lambdaService: AWSLambdaService): DBIO[Seq[BehaviorParameter]] = {
    BehaviorVersionQueries.learnCodeFor(this, code, lambdaService)
  }

  private def dropEnclosingDoubleQuotes(text: String): String = """^"|"$""".r.replaceAllIn(text, "")

  private def processedResultFor(result: JsValue): String = {
    dropEnclosingDoubleQuotes(result.as[String])
  }

  val COMMONMARK_EXTENSIONS = util.Arrays.asList(StrikethroughExtension.create, AutolinkExtension.create)

  def commonmarkParser = {
    Parser.builder().extensions(COMMONMARK_EXTENSIONS).build()
  }

  def commonmarkNodeFor(text: String): Node = {
    val node = commonmarkParser.parse(text)
    node.accept(new CommonmarkVisitor())
    node
  }

  def slackFormattedBodyTextFor(text: String): String = {
    val builder = StringBuilder.newBuilder
    val slack = new SlackRenderer(builder)
    commonmarkNodeFor(text).accept(slack)
    builder.toString
  }

  def successResultStringFor(result: JsValue, parametersWithValues: Seq[ParameterWithValue]): String = {
    val inputs = parametersWithValues.map { ea => (ea.parameter.name, JsString(ea.value)) }
    slackFormattedBodyTextFor(TemplateApplier(maybeResponseTemplate, JsDefined(result), inputs).apply)
  }

  private def handledErrorResultStringFor(json: JsValue): String = {
    val prompt = s"$ON_ERROR_PARAM triggered"
    val maybeDetail = (json \ "errorMessage").toOption.map(processedResultFor)
    Array(Some(prompt), maybeDetail).flatten.mkString(": ")
  }

  private def translateFromLambdaErrorDetails(details: String): String = {
    var translated = details
    translated = """/var/task/index.js""".r.replaceAllIn(translated, "<your function>")
    translated = """at fn|at exports\.handler""".r.replaceAllIn(translated, "at top level")
    translated
  }

  private def maybeDetailedErrorInfoIn(logResult: String): Option[String] = {
    val logRegex = """(?s).*\n.*\t.*\t(Error:.*)\n[^\n]*\nEND.*""".r
    logRegex.findFirstMatchIn(logResult).flatMap(_.subgroups.headOption).map(translateFromLambdaErrorDetails)
  }

  private def unhandledErrorResultStringFor(logResult: String): String = {
    val prompt = s"We hit an error before calling $ON_SUCCESS_PARAM or $ON_ERROR_PARAM"
    Array(Some(prompt), maybeDetailedErrorInfoIn(logResult)).flatten.mkString(":\n\n")
  }

  private def noCallbackTriggeredResultString: String = {
    s"It looks like neither callback was triggered — you need to make sure that `$ON_SUCCESS_PARAM` is called to end every successful invocation and `$ON_ERROR_PARAM` is called to end every unsuccessful one"
  }

  private def syntaxErrorResultStringFor(json: JsValue, logResult: String): String = {
    s"""
       |There's a syntax error in your function:
       |
       |${(json \ "errorMessage").asOpt[String].getOrElse("")}
       |${maybeDetailedErrorInfoIn(logResult).getOrElse("")}
     """.stripMargin
  }

  private def isUnhandledError(json: JsValue): Boolean = {
    (json \ "errorMessage").toOption.flatMap { m =>
      "Process exited before completing request".r.findFirstIn(m.toString)
    }.isDefined
  }

  private def isSyntaxError(json: JsValue): Boolean = {
    (json \ "errorType").toOption.flatMap { m =>
      "SyntaxError".r.findFirstIn(m.toString)
    }.isDefined
  }

  def resultStringFor(payload: ByteBuffer, logResult: String, parametersWithValues: Seq[ParameterWithValue]): String = {
    val bytes = payload.array
    val jsonString = new java.lang.String( bytes, Charset.forName("UTF-8") )
    val json = Json.parse(jsonString)
    (json \ "result").toOption.map { successResult =>
      successResultStringFor(successResult, parametersWithValues)
    }.getOrElse {
      if (isUnhandledError(json)) {
        unhandledErrorResultStringFor(logResult)
      } else if (json.toString == "null") {
        noCallbackTriggeredResultString
      } else if (isSyntaxError(json)) {
        syntaxErrorResultStringFor(json, logResult)
      } else {
          handledErrorResultStringFor(json)
      }
    }
  }

  def save: DBIO[BehaviorVersion] = BehaviorVersionQueries.save(this)

  def toRaw: RawBehaviorVersion = {
    RawBehaviorVersion(id, team.id, maybeDescription, maybeShortName, maybeFunctionBody, maybeResponseTemplate, createdAt)
  }

}

case class RawBehaviorVersion(
                        id: String,
                        teamId: String,
                        maybeDescription: Option[String],
                        maybeShortName: Option[String],
                        maybeFunctionBody: Option[String],
                        maybeResponseTemplate: Option[String],
                        createdAt: DateTime
                        )

class BehaviorVersionsTable(tag: Tag) extends Table[RawBehaviorVersion](tag, "behavior_versions") {

  def id = column[String]("id", O.PrimaryKey)
  def teamId = column[String]("team_id")
  def maybeDescription = column[Option[String]]("description")
  def maybeShortName = column[Option[String]]("short_name")
  def maybeFunctionBody = column[Option[String]]("code")
  def maybeResponseTemplate = column[Option[String]]("response_template")
  def createdAt = column[DateTime]("created_at")

  def * =
    (id, teamId, maybeDescription, maybeShortName, maybeFunctionBody, maybeResponseTemplate, createdAt) <>
      ((RawBehaviorVersion.apply _).tupled, RawBehaviorVersion.unapply _)
}

object BehaviorVersionQueries {

  def all = TableQuery[BehaviorVersionsTable]
  def allWithTeam = all.join(Team.all).on(_.teamId === _.id)

  def tuple2BehaviorVersion(tuple: (RawBehaviorVersion, Team)): BehaviorVersion = {
    val raw = tuple._1
    BehaviorVersion(
      raw.id,
      tuple._2,
      raw.maybeDescription,
      raw.maybeShortName,
      raw.maybeFunctionBody,
      raw.maybeResponseTemplate,
      raw.createdAt
    )
  }

  def uncompiledFindQuery(id: Rep[String]) = {
    allWithTeam.filter { case(behavior, team) => behavior.id === id }
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  // doesn't check if accessible to a user so private
  private def find(id: String): DBIO[Option[BehaviorVersion]] = {
    findQuery(id).result.map(_.headOption.map(tuple2BehaviorVersion))
  }

  def find(id: String, user: User): DBIO[Option[BehaviorVersion]] = {
    for {
      maybeBehaviorVersion <- find(id)
      maybeAccessibleBehaviorVersion <- maybeBehaviorVersion.map { behaviorVersion =>
        user.canAccess(behaviorVersion.team).map { canAccess =>
          if (canAccess) {
            Some(behaviorVersion)
          } else {
            None
          }
        }
      }.getOrElse(DBIO.successful(None))
    } yield maybeAccessibleBehaviorVersion
  }

  def uncompiledAllForTeamQuery(teamId: Rep[String]) = {
    allWithTeam.
      filter { case(behavior, team) => team.id === teamId }
  }
  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

  def allForTeam(team: Team): DBIO[Seq[BehaviorVersion]] = {
    allForTeamQuery(team.id).result.map { tuples => tuples.map(tuple2BehaviorVersion) }
  }

  def createFor(team: Team): DBIO[BehaviorVersion] = {
    val raw = RawBehaviorVersion(IDs.next, team.id, None, None, None, None, DateTime.now)

    (all += raw).map { _ =>
      BehaviorVersion(raw.id, team, raw.maybeDescription, raw.maybeShortName, raw.maybeFunctionBody, raw.maybeResponseTemplate, raw.createdAt)
    }
  }

  def uncompiledFindQueryFor(id: Rep[String]) = all.filter(_.id === id)
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def save(behaviorVersion: BehaviorVersion): DBIO[BehaviorVersion] = {
    val raw = behaviorVersion.toRaw
    val query = findQueryFor(raw.id)
    query.result.flatMap { r =>
      r.headOption.map { existing =>
        query.update(raw)
      }.getOrElse(all += raw)
    }.map(_ => behaviorVersion)
  }

  def delete(behaviorVersion: BehaviorVersion): DBIO[BehaviorVersion] = {
    all.filter(_.id === behaviorVersion.id).delete.map(_ => behaviorVersion)
  }

  private def paramsIn(code: String): Array[String] = {
    """.*function\s*\(([^\)]*)\)""".r.findFirstMatchIn(code).flatMap { firstMatch =>
      firstMatch.subgroups.headOption.map { paramString =>
        paramString.split("""\s*,\s*""").filter(_.nonEmpty)
      }
    }.getOrElse(Array())
  }

  import services.AWSLambdaConstants._

  def withoutBuiltin(params: Array[String]) = params.filterNot(ea => ea == ON_SUCCESS_PARAM || ea == ON_ERROR_PARAM || ea == CONTEXT_PARAM)

  val functionBodyRegex = """(?s)^\s*function\s*\([^\)]*\)\s*\{(.*)\}$""".r

  def learnCodeFor(behaviorVersion: BehaviorVersion, code: String, lambdaService: AWSLambdaService): DBIO[Seq[BehaviorParameter]] = {
    val actualParams = paramsIn(code)
    val paramsWithoutBuiltin = withoutBuiltin(actualParams)
    val functionBody = functionBodyRegex.findFirstMatchIn(code).flatMap { m =>
      m.subgroups.headOption
    }.getOrElse("")
    (for {
      _ <- DBIO.from(lambdaService.deployFunctionFor(behaviorVersion, functionBody, paramsWithoutBuiltin))
      b <- behaviorVersion.copy(maybeFunctionBody = Some(functionBody)).save
      params <- BehaviorParameterQueries.ensureFor(b, paramsWithoutBuiltin.map(ea => (ea, None)))
    } yield params) transactionally
  }

}
