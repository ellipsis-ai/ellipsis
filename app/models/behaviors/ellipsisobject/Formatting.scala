package models.behaviors.ellipsisobject

import ai.x.play.json.Jsonx
import json.Formatting._
import play.api.libs.json._

object Formatting {

  lazy implicit val identityInfoFormat = Jsonx.formatCaseClass[IdentityInfo]
  lazy implicit val eventUserFormat = Jsonx.formatCaseClass[EventUser]
  lazy implicit val channelInfoFormat = Jsonx.formatCaseClass[Channel]
  lazy implicit val messageInfoFormat = Jsonx.formatCaseClass[MessageObject]
  lazy implicit val scheduleInfoFormat = Jsonx.formatCaseClass[ScheduleInfo]
  lazy implicit val eventInfoFormat = Jsonx.formatCaseClass[EventInfo]
  lazy implicit val AWSConfigInfoFormat = Jsonx.formatCaseClass[AWSConfigInfo]
  lazy implicit val teamInfoFormat = Jsonx.formatCaseClass[TeamInfo]
  lazy implicit val deprecatedMessageInfoFormat = Jsonx.formatCaseClass[DeprecatedMessageInfo]
  lazy implicit val deprecatedUserInfoFormat = Jsonx.formatCaseClass[DeprecatedUserInfo]
  lazy implicit val inputInfoFormat = Jsonx.formatCaseClass[InputInfo]
  lazy implicit val defaultStorageFieldInfoFormat = Jsonx.formatCaseClass[DefaultStorageFieldInfo]

  lazy implicit val libraryInfoFormat = Jsonx.formatCaseClass[LibraryInfo]
  lazy implicit val dataTypeInfoFormat = Jsonx.formatCaseClass[DataTypeInfo]
  lazy implicit val actionInfoFormat = Jsonx.formatCaseClass[ActionInfo]
  // we want to try ActionInfo last, otherwise a data type could be interpreted as an Action
  lazy implicit val behaviorInfoFormat: Format[BehaviorInfo] = Jsonx.formatSealedWithFallback[BehaviorInfo, ActionInfo]

  lazy implicit val skillInfoFormat = Jsonx.formatCaseClass[SkillInfo]
  lazy implicit val currentActionInfoFormat = Jsonx.formatCaseClass[MetaInfo]
  lazy implicit val argInfoFormat = Jsonx.formatCaseClass[ArgInfo]
  lazy implicit val ellipsisObjectFormat = Jsonx.formatCaseClass[EllipsisObject]
}

