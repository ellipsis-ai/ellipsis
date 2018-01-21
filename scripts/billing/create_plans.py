#!/usr/bin/python
import sys
import chargebee

plans = [
  {
    "id" : "developer-free-v1",
    "name" : "Developer",
    "invoice_name" : "Developer Free Plan",
    "price" : 0,
    "currency_code": "USD"
  },
  {
    "id" : "starter-v1",
    "name" : "Starter",
    "invoice_name" : "Stater Plan v1",
    "Description" : "The best plan for small organizations. $5/month/active user.",
    "price" : 0,
    "currency_code": "USD"
  },
  {
    "id" : "business-v1",
    "name" : "Business",
    "invoice_name" : "Business Plan v1",
    "Description" : "The best plan for organizations with 50+ people. $8/month/active user.",
    "price" : 0,
    "currency_code": "USD"
  },
  {
    "id" : "enterprise-v1",
    "name" : "Enterprise",
    "invoice_name" : "Enterprise Plan v1",
    "Description" : "The best plan for Enterprise that need the maximum value.",
    "price" : 0,
    "currency_code": "USD"
  },
  {
    "id" : "expert-package-1-v1",
    "name" : "Expert Package - 1 Skill",
    "invoice_name" : "Expert Package - 1 Skill",
    "Description" : "One custom Skill with support for 12 months.",
    "period": 1,
    "period_unit": "year",
    "price" : 5000,
    "currency_code": "USD"
  },
  {
    "id" : "expert-package-3-v1",
    "name" : "Expert Package - 3 Skills",
    "invoice_name" : "Expert Package - 3 Skills",
    "Description" : "3 custom Skill with support for 12 months.",
    "period": 1,
    "period_unit": "year",
    "price" : 12000,
    "currency_code": "USD"
  },
  {
    "id" : "expert-package-enterprise-v1",
    "name" : "Expert Package - Enterprise",
    "invoice_name" : "Expert Package - Enterprise",
    "Description" : "The ultimate package for custom Skills",
    "period": 1,
    "period_unit": "year",
    "price" : 50000,
    "currency_code": "USD"
  }
]

chargebee.configure("test_O0nTVksTuwXi9mevZXdID2M7Bznw0vMj","ellipsis-test")

for plan in plans:
    result = chargebee.Plan.create(plan)
    plan = result.plan
    print(plan)
