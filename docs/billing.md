# BILLING


## Plans
Developer Plan - Free plan, up to 3 active user per month
Starter Plan - $5/active user/month, minimum $25/month
Business Plan - $8/active user/month, minimum $80/month
Enterprise Plan - call us

Skills Packages Plan:
Starter Plan - $5,000 for one skill
Business Plan - $12,000 for three skills
Enterprise Plan - call us

We want to use plans to charge for Skill Packages because each package comes with support and we want to encourage
customers to renew the Skill Package every year.


## Billing Model
We support a metered billing model plus a regular flat fee plan model for the Skills packages. A new customer chooses
first a platform plan then, optionally, she can purchase a subscription to a Skill Package plan.


## Data Model
The models are stored in Chargebee Service. Mapping between an Organization record and a Chargebee Customer is achieved
via the field chargebee_customer_id in the organizations entity.
One Ellipsis Organization entity can have multiple Subscriptions but only one of them will be to a metered plan.

## Metered Billing
Every 10 minutes we run an actor that fetches the list of pending invoices and process them.
