package json

case class DashboardData(
                          installedWorkflows: Seq[DashboardDataPoint],
                          activeWorkflows: Seq[DashboardDataPoint],
                          installedSkills: Seq[DashboardDataPoint],
                          activeSkills: Seq[DashboardDataPoint],
                          createdSkills: Seq[DashboardDataPoint],
                          modifiedSkills: Seq[DashboardDataPoint],
                          totalUsers: Seq[DashboardDataPoint],
                          activeUsers: Seq[DashboardDataPoint],
                          contributingUsers: Seq[DashboardDataPoint],
                          editingUsers: Seq[DashboardDataPoint]
                        )
