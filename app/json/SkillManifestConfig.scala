package json

case class SkillManifestConfig(containerId: String, csrfToken: String, isAdmin: Boolean, teamId: String, items: Seq[SkillManifestItemData])

object SkillManifestConfig {
  val demoData = Seq(SkillManifestItemData(
    name = "Food Safety",
    id = Some("E5--ZnuhRrqjgEapQIhimg"),
    editor = "Isabel Chamberlain",
    description = "Report and track food safety incidents",
    active = false,
    developmentStatus = "Production",
    managed = true,
    lastUsed = "Oct 2018 – Dec 2018"
  ),
    SkillManifestItemData(
      name = "Production Reports",
      id = Some("z4N1ITXRRImhInqti2pwUQ"),
      editor = "Emy Kelty",
      description = "Collect and post production reports on schedule",
      active = false,
      developmentStatus = "Production",
      managed = true,
      lastUsed = "May 2018 – Feb 2019"
    ),
    SkillManifestItemData(
      name = "Seedling Germination Checklist & Reminder",
      id = Some("IxofxQGJTZ6Z8tNu6EefWw"),
      editor = "Jessica Kowalski",
      description = "Checklist for process in germination, results stored in Google Sheets and a reminder set to unload germination chambers.",
      active = false,
      developmentStatus = "Production",
      managed = true,
      lastUsed = "Sept 2018 – Dec 2018"
    ),
    SkillManifestItemData(
      name = "Sensory QC Pre-check",
      id = Some("a2GyCpj9QCSqQy94IOlgtQ"),
      editor = "Molly Kreykes",
      description = "Collect results of sensory testing for in-progress crops.",
      active = false,
      developmentStatus = "Production",
      managed = true,
      lastUsed = "May 2018 – Sept 2018"
    ),
    SkillManifestItemData(
      name = "Standup",
      id = Some("6cf3NCiKQcGUAYx7ogvdsA"),
      editor = "Perry Skorcz",
      description = "Run standup",
      active = false,
      developmentStatus = "Production",
      managed = true,
      lastUsed = "Oct 2017 – Dec 2018"
    ),
    SkillManifestItemData(
      name = "SSF Root Cause Report",
      id = Some("NCrWrrSIT4m4RXvYxBx4gA"),
      editor = "Gabriella Carne",
      description = "Create a failure root cause report",
      active = false,
      developmentStatus = "Production",
      managed = true,
      lastUsed = "Apr 2019"
    ),
    SkillManifestItemData(
      name = "SSF Seedling Systems Checklist",
      id = Some("SsbH1p6DR9WuHi5SPJewbg"),
      editor = "Jessica Kowalski",
      description = "Check list for the seedling system",
      active = false,
      developmentStatus = "Production",
      managed = true,
      lastUsed = "Apr 2019"
    ),
    SkillManifestItemData(
      name = "SSF Systems Tracking",
      id = Some("diUzAcYbQI2qJrbbhnnWjw"),
      editor = "Teryl Chapel",
      description = "Daily tracking of downtime across all Plenty systems in SSF",
      active = false,
      developmentStatus = "Production",
      managed = true,
      lastUsed = "Apr 2019"
    ),
    SkillManifestItemData(
      name = "Accidents",
      id = Some("eEbZigZ3QBe75pgalHF2sQ"),
      editor = "Yashira Frederick",
      description = "In case of an accident, tell an employee where is the closes urgent care facility",
      active = true,
      developmentStatus = "Production",
      managed = true,
      lastUsed = "Apr 2019"
    ),
    SkillManifestItemData(
      name = "CEO Briefing",
      id = Some("EkWR_K3_TnuQHVHIIpuxbg"),
      editor = "Jennie Chen",
      description = "Collect and track items for the CEO briefing agenda.",
      active = true,
      developmentStatus = "Production",
      managed = true,
      lastUsed = "Apr 2019"
    ),
    SkillManifestItemData(
      name = "Change Management",
      id = Some("PiapjCvVQFKc-Z71QBgl3w"),
      editor = "Gabriella Carne",
      description = "Track farm ops change requests and approval",
      active = true,
      developmentStatus = "Production",
      managed = true,
      lastUsed = "Apr 2019"
    ),
    SkillManifestItemData(
      name = "Farm Standup",
      id = Some("d8TFaKU7RHaOzC665hPpzA"),
      editor = "Jessica Kowalski",
      description = "Run customized standup for the Farm team",
      active = true,
      developmentStatus = "Production",
      managed = true,
      lastUsed = "Apr 2019"
    ),
    SkillManifestItemData(
      name = "Farm Training",
      id = Some("3xD5y4YnRPuvYJ9VEz5K2w"),
      editor = "Jessica Kowalski",
      description = "Track expired training sessions from a Google sheet and send notifications in Slack reminding people to redo training sessions.",
      active = true,
      developmentStatus = "Production",
      managed = true,
      lastUsed = "Apr 2019"
    ),
    SkillManifestItemData(
      name = "Give Kudos",
      id = Some("KRAcw-NNSqi9_l09KUVrTw"),
      editor = "Chris Michael",
      description = "Recognize and celebrate co-workers",
      active = true,
      developmentStatus = "Production",
      managed = true,
      lastUsed = "Apr 2019"
    ),
    SkillManifestItemData(
      name = "Growers Journal",
      id = Some("OY-nLppwQ1-xyBrnCEJxTQ"),
      editor = "Gabriella Carne",
      description = "Daily journal of farm issues observed by growers",
      active = true,
      developmentStatus = "Production",
      managed = true,
      lastUsed = "Apr 2019"
    ),
    SkillManifestItemData(
      name = "My Calendar",
      id = Some("LIO4h-8DQRaj8EDNTPc5hQ"),
      editor = "Jessica Kowalski",
      description = "Report to the Farm team all upcoming facilities on-call schedules, weekend shifts, and PTO events ",
      active = true,
      developmentStatus = "Production",
      managed = true,
      lastUsed = "Apr 2019"
    ),
    SkillManifestItemData(
      name = "Sensory Results Checklist",
      id = Some("Rd2aHMmdQum-OoL9hvgbwg"),
      editor = "Molly Kreykes",
      description = "Collect results of sensory testing before product release.",
      active = true,
      developmentStatus = "Production",
      managed = true,
      lastUsed = "Apr 2019"
    ),
    SkillManifestItemData(
      name = "Systems Checklists",
      id = Some("jew0cCeGQGu3O3BcF1MYwA"),
      editor = "Jessica Kowalski",
      description = "Ensure that the correct process is followed in production and research grow rooms and other facilities.",
      active = true,
      developmentStatus = "Production",
      managed = true,
      lastUsed = "Apr 2019"
    ),
    SkillManifestItemData(
      name = "Work Requests",
      id = Some("xWwqXeF6RSeiY_2xPHEoyQ"),
      editor = "Gantt Charping",
      description = "Support for creating work requests. These are saved as fiix.com tasks",
      active = true,
      developmentStatus = "Production",
      managed = true,
      lastUsed = "Apr 2019"
    ),
    SkillManifestItemData(
      name = "Handbook",
      id = Some("UIdytfgFR2WK9CKbAu2aew"),
      editor = "Emy Kelty",
      description = "Simple Q&A",
      active = true,
      developmentStatus = "Production",
      managed = false,
      lastUsed = "Apr 2019"
    ),
    SkillManifestItemData(
      name = "Empathic Bot",
      id = Some("cmt3ii2lS2qwt1Nc_wekLg"),
      editor = "Ellipsis",
      description = "Greet users",
      active = true,
      developmentStatus = "Production",
      managed = false,
      lastUsed = "Apr 2019"
    ),
    SkillManifestItemData(
      name = "SF OM Requests",
      id = Some("82hUKRbET6CdRSViSeqeeQ"),
      editor = "Amanda Cabrera",
      description = "Quickly add office management requests made in Slack and store them in a Google Sheet. Allow marking them as complete via Slack to make tracking easier.",
      active = false,
      developmentStatus = "Development",
      managed = true,
      lastUsed = "Apr 2019"
    ),
    SkillManifestItemData(
      name = "Work Request Scheduling",
      id = Some("Ts6sUxF2QkG5iHUtxy9cVw"),
      editor = "Jessica Kowalski",
      description = "Collect work request information for scheduling and planning",
      active = false,
      developmentStatus = "Development",
      managed = true,
      lastUsed = "Apr 2019"
    ),
    SkillManifestItemData(
      name = "Donation Tracker",
      id = None,
      editor = "Jessica Kowalski",
      description = "Report how much produce has been donated each week ",
      active = false,
      developmentStatus = "Requested",
      managed = true,
      lastUsed = "—"
    )
  )

  def buildForDemoData(containerId: String, csrfToken: String, isAdmin: Boolean, teamId: String): SkillManifestConfig = {
    SkillManifestConfig(containerId, csrfToken, isAdmin, teamId, demoData)
  }
}
