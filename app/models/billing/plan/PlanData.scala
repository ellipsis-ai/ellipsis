package models.billing.plan


case class PlanData(
                     id: String,
                     name: String,
                     invoiceName: String,
                     description: String,
                     currencyCode: String = "USD",
                     price: Int = 0)

object StandardPlans {

  val list = Seq(
    PlanData(
      id = "developer-free-v1",
      name = "Developer(v1)",
      invoiceName = "Developer Plan (v1)",
      description =  "The best plan to get to know Ellipsis."
    ),
    PlanData(
      id = "starter-v1",
      name = "Starter(v1)",
      invoiceName = "Stater Plan (v1)",
      description =  "The best plan for small organizations. $5/month/active user."
    ),
    PlanData(
      id = "business-v1",
      name = "Business(v1)",
      invoiceName = "Business Plan (v1)",
      description =  "The best plan for organizations with 50+ people. $8/month/active user."
    ),
    PlanData(
      id = "enterprise-v1",
      name = "Enterprise(v1)",
      invoiceName = "Enterprise Plan (v1)",
      description =  "The best plan for Enterprise that need the maximum value."
    ),
    PlanData(
      id = "expert-package-1-v1",
      name = "Expert Package - 1 Skill(v1)",
      invoiceName = "Expert Package - 1 Skill(v1)",
      description =  "One custom Skill with support for 12 months."
    ),
    PlanData(
      id = "expert-package-3-v1",
      name = "Expert Package - 3 Skills(v1)",
      invoiceName = "Expert Package - 3 Skills(v1)",
      description =  "3 custom Skill with support for 12 months."
    ),
    PlanData(
      id = "expert-package-enterprise-v1",
      name = "Expert Package - Enterprise(v1)",
      invoiceName = "Expert Package - Enterprise(v1)",
      description =  "The ultimate package for custom Skills."
    )
  )
}
