package utils

import java.time.OffsetDateTime

import json._
import json.Formatting._
import models.team.Team
import play.api.libs.json._
import services.DataService

case class GithubBehaviorGroupDataBuilder(
                                data: JsValue,
                                team: Team,
                                maybeBranch: Option[String],
                                dataService: DataService
                              ) {

  val groupPath: String = nameFor(data)

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

  private def githubUrlForGroupPath: String = {
    s"${WEB_URL}/${USER_NAME}/${REPO_NAME}/tree/$branch/published/$groupPath"
  }

  private def githubUrlForBehaviorPath(behaviorType: String, behaviorPath: String): String = {
    s"${githubUrlForGroupPath}/$behaviorType/$behaviorPath"
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
    val githubUrl = githubUrlForBehaviorPath(behaviorType, nameFor(json))
    BehaviorVersionData.fromStrings(
      team.id,
      maybeDescription,
      function,
      response,
      params,
      triggers,
      config,
      Some(githubUrl),
      dataService
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

  def build: BehaviorGroupData = {
    val entries = (data \ "object" \ "entries")
    val maybeConfig = for {
      entry <- findEntryNamed("config.json", entries)
      text <- (entry \ "object" \ "text").asOpt[String]
      cfg <- Json.parse(text).asOpt[BehaviorGroupConfig]
    } yield cfg
    val maybeExportId = maybeConfig.flatMap(_.exportId)
    val name = maybeConfig.map(_.name).getOrElse(groupPath)
    val icon = maybeConfig.flatMap(_.icon)
    val requiredAWSConfigData = maybeConfig.map(_.requiredAWSConfigs).getOrElse(Seq())
    val requiredOAuth2ApiConfigData = maybeConfig.map(_.requiredOAuth2ApiConfigs).getOrElse(Seq())
    val requiredSimpleTokenApiData = maybeConfig.map(_.requiredSimpleTokenApis).getOrElse(Seq())
    val readme = findEntryNamed("README", entries).flatMap(readme => (readme \ "object" \ "text").asOpt[String])
    val githubUrl = githubUrlForGroupPath
    val actionInputs = inputsFromEntryNamed("action_inputs.json", entries)
    val dataTypeInputs = inputsFromEntryNamed("data_type_inputs.json", entries)
    val actions = behaviorVersionsDataFromEntryNamed("actions", entries)
    val dataTypes = behaviorVersionsDataFromEntryNamed("data_types", entries)
    val behaviors = actions ++ dataTypes
    val libraries = findEntryNamed("lib", entries).map(json => libraryVersionsDataFrom(json)).getOrElse(Seq())
    BehaviorGroupData(
      None,
      team.id,
      Some(name),
      readme,
      icon,
      actionInputs,
      dataTypeInputs,
      behaviors,
      libraries,
      requiredAWSConfigData,
      requiredOAuth2ApiConfigData,
      requiredSimpleTokenApiData,
      Some(githubUrl),
      maybeExportId,
      Some(OffsetDateTime.now),
      None
    )
  }

}
