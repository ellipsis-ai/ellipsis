package json

import java.time.{OffsetDateTime, ZoneOffset}

case class UsageReportConfig(
                              containerId: String,
                              csrfToken: String,
                              data: UsageReportData
                            )

object UsageReportConfig {
  def buildForDemoData(containerId: String, csrfToken: String): UsageReportConfig = {
    val installedWorkflows: Seq[BigDecimal] = Seq(
      48, 48, 48, 56, 84, 94, 106, 116, 126, 144, 149, 150,
      162, 173, 177, 177
    )
    val activeWorkflows: Seq[BigDecimal] =  Seq(
      11, 8, 13, 18, 39, 48, 36, 45, 51, 55, 57, 47,
      62, 62, 62, 47
    )
    val installedSkills: Seq[BigDecimal] = Seq(
      9, 9, 9, 12, 14, 16, 17, 22, 24, 26, 26, 27,
      28, 31, 32, 32
    )
    val activeSkills: Seq[BigDecimal] = Seq(
      5, 5, 5, 7, 7, 8, 11, 16, 17, 15, 15, 15,
      13, 15, 14, 13
    )
    val createdSkills: Seq[BigDecimal] = Seq(
      0, 0, 0, 3, 2, 2, 1, 5, 2, 2, 0, 1,
      1, 3, 1, 0
    )
    val modifiedSkills: Seq[BigDecimal] = Seq(
      1, 0, 2, 0, 4, 4, 4, 2, 7, 7, 2, 3,
      4, 3, 6, 0
    )
    val totalUsers: Seq[BigDecimal] = Seq(
      150, 166, 171, 180, 187, 211, 218, 203, 220, 230, 234, 220,
      215, 218, 225, 225
    )
    val activeUsers: Seq[BigDecimal] = Seq(
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 198, 199
    )
    val contributors: Seq[BigDecimal] = Seq(
      90, 75, 58, 62, 54, 59, 67, 62, 44, 48, 59, 52,
      65, 67, 84, 66
    )
    val editors: Seq[BigDecimal] = Seq(
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 1, 0
    )
    UsageReportConfig(containerId, csrfToken,
      UsageReportData(
        installedWorkflows = toDemoData(installedWorkflows),
        activeWorkflows = toDemoData(activeWorkflows),
        installedSkills = toDemoData(installedSkills),
        activeSkills = toDemoData(activeSkills),
        createdSkills = toDemoData(createdSkills),
        modifiedSkills = toDemoData(modifiedSkills),
        totalUsers = toDemoData(totalUsers),
        activeUsers = toDemoData(activeUsers),
        contributingUsers = toDemoData(contributors),
        editingUsers = toDemoData(editors)
      )
    )
  }

  private def toDemoData(data: Seq[BigDecimal]): Seq[ChartDataPoint] = {
    data.zipWithIndex.map {
      case (ea, index) => {
        val date = OffsetDateTime.of(2018, 1, 14, 0, 0, 0, 0, ZoneOffset.UTC)
        ChartDataPoint(
          t = date.plusMonths(index),
          y = ea
        )
      }
    }
  }
}
