package json

import export.BehaviorGroupExporter
import models.IDs
import models.behaviors.behaviorparameter.TextType
import models.behaviors.datatypefield.{DataTypeFieldForSchema, FieldTypeForSchema}

case class DataTypeFieldData(
                              id: Option[String],
                              fieldId: Option[String],
                              exportId: Option[String],
                              name: String,
                              fieldType: Option[BehaviorParameterTypeData],
                              isLabel: Boolean
                            ) extends DataTypeFieldForSchema {

  val isBuiltin: Boolean = name == "id"

  lazy val fieldTypeForSchema: FieldTypeForSchema = {
    fieldType.getOrElse {
      val paramType = TextType
      BehaviorParameterTypeData(Some(paramType.id), Some(paramType.exportId), paramType.name, Some(false))
    }
  }

  def copyForExport(groupExporter: BehaviorGroupExporter): DataTypeFieldData = {
    copy(
      id = None,
      fieldId = None,
      fieldType = fieldType.map(_.copyForExport(groupExporter))
    )
  }

  def copyWithParamTypeIdsIn(oldToNewIdMapping: collection.mutable.Map[String, String]): DataTypeFieldData = {
    val maybeOldDataTypeId = fieldType.flatMap(_.id)
    val maybeNewDataTypeId = maybeOldDataTypeId.flatMap(oldId => oldToNewIdMapping.get(oldId))
    maybeNewDataTypeId.map { newId =>
      copy(fieldType = fieldType.map(_.copy(id = Some(newId))))
    }.getOrElse(this)
  }

  def copyWithParamTypeIdFromExportId(behaviorVersionsData: Seq[BehaviorVersionData]): DataTypeFieldData = {
    val maybeMatchingBehaviorVersion = behaviorVersionsData.find(_.exportId == fieldType.flatMap(_.exportId))
    copy(fieldType = fieldType.map(_.copy(id = maybeMatchingBehaviorVersion.flatMap(_.id))))
  }

  def copyForClone: DataTypeFieldData = {
    copy(
      id = Some(IDs.next),
      fieldId = Some(IDs.next),
      exportId = Some(IDs.next)
    )
  }

  def copyForNewVersion: DataTypeFieldData = {
    copy(
      id = Some(IDs.next)
    )
  }

  def copyForImportableFor(maybeExistingGroupData: Option[BehaviorGroupData]): DataTypeFieldData = {
    val maybeExisting: Option[DataTypeFieldData] = maybeExistingGroupData.flatMap { groupData =>
      groupData.behaviorVersions.flatMap { bv =>
        bv.config.dataTypeConfig.flatMap { dtConfig =>
          dtConfig.fields.find(f => f.exportId.isDefined && f.exportId == exportId)
        }
      }.headOption
    }
    copy(
      fieldId = maybeExisting.flatMap(_.fieldId).orElse(Some(IDs.next))
    )
  }

}

object DataTypeFieldData {

  def newUnsavedNamed(name: String, paramType: BehaviorParameterTypeData): DataTypeFieldData = DataTypeFieldData(
    id = Some(IDs.next),
    fieldId = Some(IDs.next),
    exportId = None,
    name,
    Some(paramType),
    isLabel = false
  )
}
