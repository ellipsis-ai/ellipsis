package models.bots

import java.nio.ByteBuffer
import java.nio.charset.Charset
import com.github.tototoshi.slick.PostgresJodaSupport._
import json.BehaviorVersionData
import models.accounts.User
import models.bots.config.{AWSConfig, AWSConfigQueries}
import models.bots.templates.TemplateApplier
import models.bots.triggers.MessageTriggerQueries
import models.{EnvironmentVariable, EnvironmentVariableQueries, IDs, Team}
import org.commonmark.node.{Image, AbstractVisitor}
import org.joda.time.DateTime
import play.api.libs.json.{JsString, JsDefined, Json, JsValue}
import play.api.{Configuration, Play}
import services.AWSLambdaConstants._
import services.{AWSLambdaLogResult, AWSLambdaService}
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
                     behavior: Behavior,
                     maybeDescription: Option[String],
                     maybeShortName: Option[String],
                     maybeFunctionBody: Option[String],
                     maybeResponseTemplate: Option[String],
                     maybeAuthor: Option[User],
                     createdAt: DateTime
                     ) {

  val team: Team = behavior.team

  private def environmentVariablesUsedInConfig: DBIO[Seq[String]] = {
    AWSConfigQueries.maybeFor(this).map { maybeAwsConfig =>
      maybeAwsConfig.map { awsConfig =>
        awsConfig.environmentVariableNames
      }.getOrElse(Seq())
    }
  }

  def knownEnvironmentVariablesUsed: DBIO[Seq[String]] = {
    environmentVariablesUsedInConfig.map { inConfig =>
      inConfig ++ BehaviorVersionQueries.environmentVariablesUsedInCode(functionBody)
    }
  }

  def missingEnvironmentVariablesIn(environmentVariables: Seq[EnvironmentVariable]): DBIO[Seq[String]] = {
    knownEnvironmentVariablesUsed.map{ used =>
      used diff environmentVariables.filter(_.value.trim.nonEmpty).map(_.name)
    }
  }

  // TODO: make this real
  def isInDevelopmentMode: Boolean = true

  def restore: DBIO[BehaviorVersion] = {
    (for {
      newVersion <- this.copy(id = IDs.next, createdAt = DateTime.now).save
      currentTriggers <- MessageTriggerQueries.allFor(this)
      newTriggers <- DBIO.sequence(currentTriggers.map { t =>
        MessageTriggerQueries.createFor(newVersion, t.pattern, t.requiresBotMention, t.shouldTreatAsRegex, t.isCaseSensitive)
      })
      currentParams <- BehaviorParameterQueries.allFor(this)
      newParams <- DBIO.sequence(currentParams.map { p =>
        BehaviorParameterQueries.createFor(p.name, p.maybeQuestion, p.rank, newVersion)
      })
    } yield newVersion) transactionally
  }

  def isSkill: Boolean = {
    maybeFunctionBody.map { body =>
      Option(body).filter(_.trim.nonEmpty).isDefined
    }.getOrElse(false)
  }

  def editLinkFor(configuration: Configuration): Option[String] = {
    configuration.getString("application.apiBaseUrl").map { baseUrl =>
      val path = controllers.routes.ApplicationController.editBehavior(behavior.id)
      s"$baseUrl$path"
    }
  }

  def description: String = maybeDescription.getOrElse("")

  def functionBody: String = maybeFunctionBody.getOrElse("")

  def awsParamsFor(maybeAWSConfig: Option[AWSConfig]): Array[String] = {
    maybeAWSConfig.map(_ => Array("AWS")).getOrElse(Array())
  }

  def functionWithParams(params: Array[String], awsParams: Array[String]): String = {
    val definitionUserParamsString = if (params.isEmpty) {
      ""
    } else {
      s"""\n${params.map(ea => ea ++ ",").mkString("\n")}\n"""
    }
    val definitionBuiltinParamsString = (HANDLER_PARAMS ++ Array(CONTEXT_PARAM) ++ awsParams).mkString(", ")
    val possibleEndOfParamsNewline = if (params.isEmpty) { "" } else { "\n" }
    s"""function($definitionUserParamsString$definitionBuiltinParamsString$possibleEndOfParamsNewline) {
      |  $functionBody
      |}""".stripMargin
  }

  def maybeFunction: DBIO[Option[String]] = {
    maybeFunctionBody.map { functionBody =>
      BehaviorParameterQueries.allFor(this).flatMap { params =>
        AWSConfigQueries.maybeFor(this).map { maybeAWSConfig =>
          functionWithParams(params.map(_.name).toArray, awsParamsFor(maybeAWSConfig))
        }
      }.map(Some(_))
    }.getOrElse(DBIO.successful(None))
  }

  lazy val conf = Play.current.configuration

  def functionName: String = id

  def unformattedResultFor(parametersWithValues: Seq[ParameterWithValue], service: AWSLambdaService): Future[String] = {
    for {
      envVars <- service.models.run(EnvironmentVariableQueries.allFor(team))
      result <- service.invoke(this, parametersWithValues, envVars)
    } yield result
  }

  def unlearn(lambdaService: AWSLambdaService): DBIO[Unit] = {
    lambdaService.deleteFunction(id)
    BehaviorVersionQueries.delete(this).map(_ => Unit)
  }

  private def dropEnclosingDoubleQuotes(text: String): String = """^"|"$""".r.replaceAllIn(text, "")

  private def processedResultFor(result: JsValue): String = {
    dropEnclosingDoubleQuotes(result.as[String])
  }

  def unformattedSuccessResultStringFor(result: JsValue, parametersWithValues: Seq[ParameterWithValue]): String = {
    val inputs = parametersWithValues.map { ea => (ea.parameter.name, JsString(ea.value)) }
    TemplateApplier(maybeResponseTemplate, JsDefined(result), inputs).apply
  }

  private def handledErrorResultStringFor(json: JsValue): String = {
    val prompt = s"$ON_ERROR_PARAM triggered"
    val maybeDetail = (json \ "errorMessage").toOption.map(processedResultFor)
    Array(Some(prompt), maybeDetail).flatten.mkString(": ")
  }

  private def unhandledErrorResultStringFor(logResult: AWSLambdaLogResult): String = {
    val prompt = s"\nWe hit an error before calling $ON_SUCCESS_PARAM or $ON_ERROR_PARAM"
    Array(Some(prompt), logResult.maybeTranslated).flatten.mkString(":\n\n")
  }

  private def noCallbackTriggeredResultString: String = {
    s"It looks like neither callback was triggered — you need to make sure that `$ON_SUCCESS_PARAM` is called to end every successful invocation and `$ON_ERROR_PARAM` is called to end every unsuccessful one"
  }

  private def syntaxErrorResultStringFor(json: JsValue, logResult: AWSLambdaLogResult): String = {
    s"""
       |There's a syntax error in your function:
       |
       |${(json \ "errorMessage").asOpt[String].getOrElse("")}
       |${logResult.maybeTranslated.getOrElse("")}
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

  def unformattedResultStringFor(payload: ByteBuffer, logResult: AWSLambdaLogResult, parametersWithValues: Seq[ParameterWithValue]): String = {
    val bytes = payload.array
    val jsonString = new java.lang.String( bytes, Charset.forName("UTF-8") )
    val json = Json.parse(jsonString)
    val mainResultString = (json \ "result").toOption.map { successResult =>
      unformattedSuccessResultStringFor(successResult, parametersWithValues)
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
    logResult.userDefinedLogStatements ++ mainResultString
  }

  def save: DBIO[BehaviorVersion] = BehaviorVersionQueries.save(this)

  def toRaw: RawBehaviorVersion = {
    RawBehaviorVersion(id, behavior.id, maybeDescription, maybeShortName, maybeFunctionBody, maybeResponseTemplate, maybeAuthor.map(_.id), createdAt)
  }

}

case class RawBehaviorVersion(
                        id: String,
                        behaviorId: String,
                        maybeDescription: Option[String],
                        maybeShortName: Option[String],
                        maybeFunctionBody: Option[String],
                        maybeResponseTemplate: Option[String],
                        maybeAuthorId: Option[String],
                        createdAt: DateTime
                        )

class BehaviorVersionsTable(tag: Tag) extends Table[RawBehaviorVersion](tag, "behavior_versions") {

  def id = column[String]("id", O.PrimaryKey)
  def behaviorId = column[String]("behavior_id")
  def maybeDescription = column[Option[String]]("description")
  def maybeShortName = column[Option[String]]("short_name")
  def maybeFunctionBody = column[Option[String]]("code")
  def maybeResponseTemplate = column[Option[String]]("response_template")
  def maybeAuthorId = column[Option[String]]("author_id")
  def createdAt = column[DateTime]("created_at")

  def * =
    (id, behaviorId, maybeDescription, maybeShortName, maybeFunctionBody, maybeResponseTemplate, maybeAuthorId, createdAt) <>
      ((RawBehaviorVersion.apply _).tupled, RawBehaviorVersion.unapply _)
}

object BehaviorVersionQueries {

  def all = TableQuery[BehaviorVersionsTable]
  def allWithUser = all.joinLeft(User.all).on(_.maybeAuthorId === _.id)
  def allWithBehavior = allWithUser.join(BehaviorQueries.allWithTeam).on(_._1.behaviorId === _._1.id)

  def tuple2BehaviorVersion(tuple: ((RawBehaviorVersion, Option[User]), (RawBehavior, Team))): BehaviorVersion = {
    val raw = tuple._1._1
    BehaviorVersion(
      raw.id,
      BehaviorQueries.tuple2Behavior(tuple._2),
      raw.maybeDescription,
      raw.maybeShortName,
      raw.maybeFunctionBody,
      raw.maybeResponseTemplate,
      tuple._1._2,
      raw.createdAt
    )
  }

  def uncompiledAllForQuery(behaviorId: Rep[String]) = {
    allWithBehavior.
      filter { case((version, _), _) => version.behaviorId === behaviorId }.
      sortBy { case((version, _), _) => version.createdAt.desc }
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def allFor(behavior: Behavior): DBIO[Seq[BehaviorVersion]] = {
    allForQuery(behavior.id).result.map { r =>
      r.map(tuple2BehaviorVersion)
    }
  }

  def uncompiledFindQuery(id: Rep[String]) = {
    allWithBehavior.filter { case((version, _), _) => version.id === id }
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def findWithoutAccessCheck(id: String): DBIO[Option[BehaviorVersion]] = {
    findQuery(id).result.map(_.headOption.map(tuple2BehaviorVersion))
  }

  def createFor(behavior: Behavior, maybeUser: Option[User]): DBIO[BehaviorVersion] = {
    val raw = RawBehaviorVersion(IDs.next, behavior.id, None, None, None, None, maybeUser.map(_.id), DateTime.now)

    (all += raw).map { _ =>
      BehaviorVersion(raw.id, behavior, raw.maybeDescription, raw.maybeShortName, raw.maybeFunctionBody, raw.maybeResponseTemplate, maybeUser, raw.createdAt)
    }
  }

  def createFor(behavior: Behavior, maybeUser: Option[User], lambdaService: AWSLambdaService, data: BehaviorVersionData): DBIO[BehaviorVersion] = {
    (for {
      behaviorVersion <- createFor(behavior, maybeUser)
      _ <-
        for {
          updated <- behaviorVersion.copy(
            maybeFunctionBody = Some(data.functionBody),
            maybeResponseTemplate = Some(data.responseTemplate)
          ).save
          maybeAWSConfig <- data.awsConfig.map { c =>
            AWSConfigQueries.createFor(updated, c.accessKeyName, c.secretKeyName, c.regionName).map(Some(_))
          }.getOrElse(DBIO.successful(None))
          _ <- DBIO.from(lambdaService.deployFunctionFor(updated, data.functionBody, BehaviorVersionQueries.withoutBuiltin(data.params.map(_.name).toArray), maybeAWSConfig))
          _ <- BehaviorParameterQueries.ensureFor(updated, data.params.map(ea => (ea.name, Some(ea.question))))
          _ <- DBIO.sequence(
            data.triggers.
              filterNot(_.text.trim.isEmpty)
              map { trigger =>
              MessageTriggerQueries.createFor(updated, trigger.text, trigger.requiresMention, trigger.isRegex, trigger.caseSensitive)
            }
          )
        } yield Unit
    } yield behaviorVersion) transactionally
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

  def environmentVariablesUsedInCode(functionBody: String): Seq[String] = {
    // regex quite incomplete, but we're just trying to provide some guidance
    """(?s)ellipsis\.env\.([$A-Za-z_][0-9A-Za-z_$]*)""".r.findAllMatchIn(functionBody).flatMap { m =>
      m.subgroups.headOption
    }.toSeq
  }

}
