package json

case class UsageReportData(
                            installedWorkflows: Seq[ChartDataPoint],
                            activeWorkflows: Seq[ChartDataPoint],
                            installedSkills: Seq[ChartDataPoint],
                            activeSkills: Seq[ChartDataPoint],
                            createdSkills: Seq[ChartDataPoint],
                            modifiedSkills: Seq[ChartDataPoint],
                            totalUsers: Seq[ChartDataPoint],
                            activeUsers: Seq[ChartDataPoint],
                            contributingUsers: Seq[ChartDataPoint],
                            editingUsers: Seq[ChartDataPoint]
                          )
