import java.time.{OffsetDateTime, ZoneOffset}

import json.SkillManifestItemData
import models.IDs
import org.scalatestplus.play.PlaySpec

class SkillManifestItemDataSpec extends PlaySpec {
  def date(year: Int, month: Int, day: Int): OffsetDateTime = {
    OffsetDateTime.of(year, day, month, 0, 0, 0, 0, ZoneOffset.UTC)
  }

  def skill(created: OffsetDateTime, maybeLastUsed: Option[OffsetDateTime]): SkillManifestItemData = {
    SkillManifestItemData(IDs.next, None, None, IDs.next, managed = false, created, Some(created), maybeLastUsed)
  }

  val skillA = skill(date(2019, 2, 1), Some(date(2019, 1, 2)))
  val skillB = skill(date(2019, 2, 2), Some(date(2019, 1, 1)))
  val skillC = skill(date(2019, 3, 2), None)
  val skillD = skill(date(2019, 3, 1), None)

  "a list of items" should {
    "sort in ascending order by last used if present, or by created date if missing" in {
      val list = Seq(skillA, skillB, skillC, skillD)
      list.sorted mustEqual Seq(skillD, skillC, skillB, skillA)
    }
  }

}
