#!/usr/bin/python
import sys
import chargebee

plans = [
  {
    "id" : "developer-free-v1",
    "name" : "Developer(v1)",
    "invoice_name" : "Developer Plan (v1)",
    "Description" : "The best plan to get to know Ellipsis.",
    "price" : 0,
    "currency_code": "USD"
  },
  {
    "id" : "starter-v1",
    "name" : "Starter(v1)",
    "invoice_name" : "Stater Plan (v1)",
    "Description" : "The best plan for small organizations. $5/month/active user.",
    "price" : 0,
    "currency_code": "USD"
  },
  {
    "id" : "business-v1",
    "name" : "Business(v1)",
    "invoice_name" : "Business Plan (v1)",
    "Description" : "The best plan for organizations with 50+ people. $8/month/active user.",
    "price" : 0,
    "currency_code": "USD"
  },
  {
    "id" : "enterprise-v1",
    "name" : "Enterprise(v1)",
    "invoice_name" : "Enterprise Plan (v1)",
    "Description" : "The best plan for Enterprise that need the maximum value.",
    "price" : 0,
    "currency_code": "USD"
  },
  {
    "id" : "expert-package-1-v1",
    "name" : "Expert Package - 1 Skill(v1)",
    "invoice_name" : "Expert Package - 1 Skill(v1)",
    "Description" : "One custom Skill with support for 12 months.",
    "period": 1,
    "period_unit": "year",
    "price" : 5000,
    "currency_code": "USD"
  },
  {
    "id" : "expert-package-3-v1",
    "name" : "Expert Package - 3 Skills(v1)",
    "invoice_name" : "Expert Package - 3 Skills(v1)",
    "Description" : "3 custom Skill with support for 12 months.",
    "period": 1,
    "period_unit": "year",
    "price" : 1200000,
    "currency_code": "USD"
  },
  {
    "id" : "expert-package-enterprise-v1",
    "name" : "Expert Package - Enterprise(v1)",
    "invoice_name" : "Expert Package - Enterprise(v1)",
    "Description" : "The ultimate package for custom Skills.",
    "period": 1,
    "period_unit": "year",
    "price" : 5000000,
    "currency_code": "USD"
  }
]

chargebee.configure("test_O0nTVksTuwXi9mevZXdID2M7Bznw0vMj","ellipsis-test")

for plan in plans:
    result = chargebee.Plan.create(plan)
    plan = result.plan
    print(plan)
