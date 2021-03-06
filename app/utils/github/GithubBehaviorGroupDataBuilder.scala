package utils.github

import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

import json.Formatting._
import json._
import models.team.Team
import play.api.libs.json._
import services.DataService

case class GithubBehaviorGroupDataBuilder(
                                           data: JsValue,
                                           team: Team,
                                           owner: String,
                                           repoName: String,
                                           maybeBranch: Option[String],
                                           maybeSHA: Option[String],
                                           maybeTimestamp: Option[String],
                                           dataService: DataService
                                        ) {

  val API_URL = "https://api.github.com/graphql"
  val WEB_URL = "https://github.com"
  val USER_NAME = "ellipsis-ai"
  val REPO_NAME = "behaviors"

  val branch: String = maybeBranch.getOrElse("master")

  private def nameFor(json: JsValue): String = (json \ "name").as[String]

  private def maybeTextFromEntry(json: JsValue): Option[String] = {
    (json \ "object" \ "text").asOpt[String]
  }

  private def findEntryNamed(name: String, lookup: JsLookupResult): Option[JsValue] = {
    lookup match {
      case JsDefined(JsArray(arr)) => {
        arr.find(ea => (ea \ "name").asOpt[String].contains(name))
      }
      case _ => None
    }
  }

  private def inputsFor(text: String): Seq[InputData] = {
    Json.parse(text).asOpt[Seq[InputData]].getOrElse(Seq())
  }

  private def inputsFromEntryNamed(name: String, lookup: JsLookupResult): Seq[InputData] = {
    (for {
      entry <- findEntryNamed(name, lookup)
      text <- maybeTextFromEntry(entry)
    } yield inputsFor(text)).getOrElse(Seq())
  }

  private def behaviorVersionDataFrom(
                                       json: JsValue,
                                       behaviorType: String
                                     ): BehaviorVersionData = {
    val actionsLookup = (json \ "object" \ "entries")
    val maybeDescription = findEntryNamed("README", actionsLookup).flatMap(maybeTextFromEntry)
    val function = findEntryNamed("function.js", actionsLookup).flatMap(maybeTextFromEntry).getOrElse("")
    val response = findEntryNamed("response.md", actionsLookup).flatMap(maybeTextFromEntry).getOrElse("")
    val params = findEntryNamed("params.json", actionsLookup).flatMap(maybeTextFromEntry).getOrElse("")
    val triggers = findEntryNamed("triggers.json", actionsLookup).flatMap(maybeTextFromEntry).getOrElse("")
    val config = findEntryNamed("config.json", actionsLookup).flatMap(maybeTextFromEntry).getOrElse("")
    BehaviorVersionData.fromStrings(
      team.id,
      maybeDescription,
      function,
      response,
      params,
      triggers,
      config
    )
  }

  private def behaviorVersionsDataFromEntryNamed(
                                                 name: String,
                                                 lookup: JsLookupResult
                                               ): Seq[BehaviorVersionData] = {
    findEntryNamed(name, lookup).map { entry =>
      (entry \ "object" \ "entries") match {
        case JsDefined(JsArray(arr)) => arr.map(ea => behaviorVersionDataFrom(ea, name))
        case _ => Seq()
      }
    }.getOrElse(Seq())
  }

  private def libraryVersionDataFrom(json: JsValue): LibraryVersionData = {
    val content = maybeTextFromEntry(json).getOrElse("")
    LibraryVersionData.from(content, nameFor(json))
  }

  private def libraryVersionsDataFrom(json: JsValue): Seq[LibraryVersionData] = {
    (json \ "object" \ "entries") match {
      case JsDefined(JsArray(arr)) => arr.map(ea => libraryVersionDataFrom(ea))
      case _ => Seq()
    }
  }

  lazy val maybeCreatedAt: Option[OffsetDateTime] = {
    maybeTimestamp.flatMap { timestamp =>
      try {
        Some(OffsetDateTime.parse(timestamp))
      } catch {
        case _: DateTimeParseException => None
      }
    }
  }

  def build: BehaviorGroupData = {
    val entries = data \ "entries"
    val maybeConfig = for {
      entry <- findEntryNamed("config.json", entries)
      text <- (entry \ "object" \ "text").asOpt[String]
      cfg <- Json.parse(text).asOpt[BehaviorGroupConfig]
    } yield cfg
    val maybeExportId = maybeConfig.flatMap(_.exportId)
    val name = maybeConfig.map(_.name).getOrElse(repoName)
    val icon = maybeConfig.flatMap(_.icon)
    val requiredAWSConfigData = maybeConfig.flatMap(_.requiredAWSConfigs).getOrElse(Seq())
    val requiredOAuthApiConfigData = maybeConfig.flatMap(_.requiredOAuthApiConfigs).getOrElse(Seq())
    val requiredSimpleTokenApiData = maybeConfig.flatMap(_.requiredSimpleTokenApis).getOrElse(Seq())
    val readme = findEntryNamed("README", entries).flatMap(readme => (readme \ "object" \ "text").asOpt[String])
    val actionInputs = inputsFromEntryNamed("action_inputs.json", entries)
    val dataTypeInputs = inputsFromEntryNamed("data_type_inputs.json", entries)
    val actions = behaviorVersionsDataFromEntryNamed("actions", entries)
    val dataTypes = behaviorVersionsDataFromEntryNamed("data_types", entries)
    val behaviors = actions ++ dataTypes
    val libraries = findEntryNamed("lib", entries).map(json => libraryVersionsDataFrom(json)).getOrElse(Seq())
    BehaviorGroupData.fromExport(
      team.id,
      Some(name),
      readme,
      icon,
      actionInputs,
      dataTypeInputs,
      behaviors,
      libraries,
      requiredAWSConfigData,
      requiredOAuthApiConfigData,
      requiredSimpleTokenApiData,
      maybeSHA,
      maybeExportId,
      maybeAuthor = None,
      Some(LinkedGithubRepoData(owner, repoName, maybeBranch)),
      maybeCreatedAt
    )
  }

}
