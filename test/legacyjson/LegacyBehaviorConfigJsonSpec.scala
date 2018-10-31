package legacyjson

import json.{BehaviorConfig, DataTypeConfigData, DataTypeFieldData, LegacyBehaviorConfigJson}
import models.behaviors.behaviorversion.{Normal, Private}
import org.scalatestplus.play.PlaySpec

class LegacyBehaviorConfigJsonSpec extends PlaySpec {
  def newLegacy: LegacyBehaviorConfigJson = {
    LegacyBehaviorConfigJson(
      exportId = None,
      name = Some("Behavior"),
      forcePrivateResponse = None,
      responseTypeId = None,
      canBeMemoized = None,
      isDataType = false,
      isTest = None,
      dataTypeConfig = None
    )
  }

  def newExpected: BehaviorConfig = {
    BehaviorConfig(
      exportId = None,
      name = Some("Behavior"),
      responseTypeId = Normal.id,
      canBeMemoized = None,
      isDataType = false,
      isTest = None,
      dataTypeConfig = None
    )
  }

  "LegacyBehaviorConfigJson" should {
    "convert to BehaviorConfig with responseTypeId set to Private if forcePrivateResponse is true" in {
      val legacy = newLegacy.copy(forcePrivateResponse = Some(true))
      val expected = newExpected.copy(responseTypeId = Private.id)
      legacy.toBehaviorConfig mustEqual expected
    }

    "convert to BehaviorConfig with responseTypeId set to Normal if forcePrivateResponse is not true" in {
      val legacy1 = newLegacy.copy(forcePrivateResponse = Some(false))
      val legacy2 = newLegacy.copy(forcePrivateResponse = None)
      val expected = newExpected.copy(responseTypeId = Normal.id)
      legacy1.toBehaviorConfig mustEqual expected
      legacy2.toBehaviorConfig mustEqual expected
    }

    "use the existing responseTypeId if provided" in {
      val legacy = newLegacy.copy(forcePrivateResponse = Some(true), responseTypeId = Some(Normal.id))
      val expected = newExpected.copy(responseTypeId = Normal.id)
      legacy.toBehaviorConfig mustEqual expected
    }

    "add a default data type config if none is provided and isDataType is true" in {
      val legacy = newLegacy.copy(isDataType = true)
      val expected = newExpected.copy(isDataType = true, dataTypeConfig = Some(DataTypeConfigData.withDefaultSettings))
      legacy.toBehaviorConfig mustEqual expected
    }

    "maintain the existing data type config if one is provided" in {
      val dataTypeConfigData = DataTypeConfigData(Seq(DataTypeFieldData(None, Some("fieldId"), None, "Field", fieldType = None, isLabel = false)), usesCode = Some(false))
      val legacy = newLegacy.copy(isDataType = true, dataTypeConfig = Some(dataTypeConfigData))
      val expected = newExpected.copy(isDataType = true, dataTypeConfig = Some(dataTypeConfigData))
      legacy.toBehaviorConfig mustEqual expected
    }
  }
}
