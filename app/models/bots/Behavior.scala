package models.bots

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util

import _root_.util.TemplateApplier
import com.github.tototoshi.slick.PostgresJodaSupport._
import models.{EnvironmentVariableQueries, IDs, Team}
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.node.{Image, AbstractVisitor, Node}
import org.commonmark.parser.Parser
import org.joda.time.DateTime
import play.api.libs.json.{JsDefined, Json, JsValue}
import play.api.{Configuration, Play}
import renderers.SlackRenderer
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

case class Behavior(
                     id: String,
                     team: Team,
                     maybeDescription: Option[String],
                     maybeShortName: Option[String],
                     maybeFunctionBody: Option[String],
                     maybeResponseTemplate: Option[String],
                     createdAt: DateTime
                     ) {

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

  def resultFor(params: Map[String, String], service: AWSLambdaService): Future[String] = {
    for {
      envVars <- service.models.run(EnvironmentVariableQueries.allFor(team))
      result <- service.invoke(this, params, envVars)
    } yield result
  }

  def unlearn(lambdaService: AWSLambdaService): DBIO[Unit] = {
    lambdaService.deleteFunction(id)
    BehaviorQueries.delete(this).map(_ => Unit)
  }

  def learnCode(code: String, lambdaService: AWSLambdaService): DBIO[Seq[BehaviorParameter]] = {
    BehaviorQueries.learnCodeFor(this, code, lambdaService)
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

  private def successResultStringFor(result: JsValue): String = {
    slackFormattedBodyTextFor(TemplateApplier(maybeResponseTemplate, JsDefined(result)).apply)
  }

  private def handledErrorResultStringFor(json: JsValue): String = {
    val prompt = s"$ON_ERROR_PARAM triggered"
    val maybeDetail = (json \ "errorMessage").toOption.map(processedResultFor)
    Array(Some(prompt), maybeDetail).flatten.mkString(": ")
  }

  private def unhandledErrorResultStringFor(logResult: String): String = {
    val prompt = s"We hit an error before calling $ON_SUCCESS_PARAM or $ON_ERROR_PARAM"
    val logRegex = """.*\n.*\t.*\t(.*)""".r
    val maybeDetail = logRegex.findFirstMatchIn(logResult).flatMap(_.subgroups.headOption)
    Array(Some(prompt), maybeDetail).flatten.mkString(": ")
  }

  private def isUnhandledError(json: JsValue): Boolean = {
    (json \ "errorMessage").toOption.flatMap { m =>
      "Process exited before completing request".r.findFirstIn(m.toString)
    }.isDefined
  }

  def resultStringFor(payload: ByteBuffer, logResult: String): String = {
    val bytes = payload.array
    val jsonString = new java.lang.String( bytes, Charset.forName("UTF-8") )
    val json = Json.parse(jsonString)
    (json \ "result").toOption.map { successResult =>
      successResultStringFor(successResult)
    }.getOrElse {
      if (isUnhandledError(json)) {
        unhandledErrorResultStringFor(logResult)
      } else {
        handledErrorResultStringFor(json)
      }
    }
  }

  def save: DBIO[Behavior] = BehaviorQueries.save(this)

  def toRaw: RawBehavior = {
    RawBehavior(id, team.id, maybeDescription, maybeShortName, maybeFunctionBody, maybeResponseTemplate, createdAt)
  }

}

case class RawBehavior(
                        id: String,
                        teamId: String,
                        maybeDescription: Option[String],
                        maybeShortName: Option[String],
                        maybeFunctionBody: Option[String],
                        maybeResponseTemplate: Option[String],
                        createdAt: DateTime
                        )

class BehaviorsTable(tag: Tag) extends Table[RawBehavior](tag, "behaviors") {

  def id = column[String]("id", O.PrimaryKey)
  def teamId = column[String]("team_id")
  def maybeDescription = column[Option[String]]("description")
  def maybeShortName = column[Option[String]]("short_name")
  def maybeFunctionBody = column[Option[String]]("code")
  def maybeResponseTemplate = column[Option[String]]("response_template")
  def createdAt = column[DateTime]("created_at")

  def * =
    (id, teamId, maybeDescription, maybeShortName, maybeFunctionBody, maybeResponseTemplate, createdAt) <>
      ((RawBehavior.apply _).tupled, RawBehavior.unapply _)
}

object BehaviorQueries {

  def all = TableQuery[BehaviorsTable]
  def allWithTeam = all.join(Team.all).on(_.teamId === _.id)

  def tuple2Behavior(tuple: (RawBehavior, Team)): Behavior = {
    val raw = tuple._1
    Behavior(
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

  def find(id: String): DBIO[Option[Behavior]] = {
    findQuery(id).result.map(_.headOption.map(tuple2Behavior))
  }

  def uncompiledAllForTeamQuery(teamId: Rep[String]) = {
    allWithTeam.
      filter { case(behavior, team) => team.id === teamId }
  }
  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

  def allForTeam(team: Team): DBIO[Seq[Behavior]] = {
    allForTeamQuery(team.id).result.map { tuples => tuples.map(tuple2Behavior) }
  }

  def createFor(team: Team): DBIO[Behavior] = {
    val raw = RawBehavior(IDs.next, team.id, None, None, None, None, DateTime.now)

    (all += raw).map { _ =>
      Behavior(raw.id, team, raw.maybeDescription, raw.maybeShortName, raw.maybeFunctionBody, raw.maybeResponseTemplate, raw.createdAt)
    }
  }

  def uncompiledFindQueryFor(id: Rep[String]) = all.filter(_.id === id)
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def save(behavior: Behavior): DBIO[Behavior] = {
    val raw = behavior.toRaw
    val query = findQueryFor(raw.id)
    query.result.flatMap { r =>
      r.headOption.map { existing =>
        query.update(raw)
      }.getOrElse(all += raw)
    }.map(_ => behavior)
  }

  def delete(behavior: Behavior): DBIO[Behavior] = {
    all.filter(_.id === behavior.id).delete.map(_ => behavior)
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

  def learnCodeFor(behavior: Behavior, code: String, lambdaService: AWSLambdaService): DBIO[Seq[BehaviorParameter]] = {
    val actualParams = paramsIn(code)
    val paramsWithoutBuiltin = withoutBuiltin(actualParams)
    val functionBody = functionBodyRegex.findFirstMatchIn(code).flatMap { m =>
      m.subgroups.headOption
    }.getOrElse("")
    (for {
      _ <- DBIO.from(lambdaService.deployFunctionFor(behavior, functionBody, paramsWithoutBuiltin))
      b <- behavior.copy(maybeFunctionBody = Some(functionBody)).save
      params <- BehaviorParameterQueries.ensureFor(b, paramsWithoutBuiltin.map(ea => (ea, None)))
    } yield params) transactionally
  }

}
