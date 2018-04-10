
import java.time.OffsetDateTime

import models.IDs
import models.team.Team
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsValue, Json}
import support.TestContext
import utils.github.GithubBehaviorGroupDataBuilder

class GithubBehaviorGroupDataBuilderSpec extends PlaySpec with MockitoSugar {

  val team: Team = Team("Test team")

  "build" should {

    "build BehaviorGroupData from JSON" in new TestContext {
      val json: JsValue = Json.parse("""{"name":"GitHub","entries":[{"name":"README","object":{"text":"Quickly view and create issues, pull requests and more"}},{"name":"action_inputs.json","object":{"text":"[ {\n  \"exportId\" : \"2c2abbQ9Re-gfyepkhSC3Q\",\n  \"name\" : \"title\",\n  \"paramType\" : {\n    \"exportId\" : \"Text\",\n    \"name\" : \"Text\"\n  },\n  \"question\" : \"What's the title of the issue?\",\n  \"isSavedForTeam\" : false,\n  \"isSavedForUser\" : false\n}, {\n  \"exportId\" : \"TnqANnwwQZGGcx4GlBRzqw\",\n  \"name\" : \"repo\",\n  \"paramType\" : {\n    \"exportId\" : \"vne9aZ6eQ_6hOFwnSm3alg\",\n    \"name\" : \"GithubRepo\"\n  },\n  \"question\" : \"Which repo? (I'll remember next time)\",\n  \"isSavedForTeam\" : false,\n  \"isSavedForUser\" : true\n}, {\n  \"exportId\" : \"sbTJmKljQButl8kY_5TQCQ\",\n  \"name\" : \"body\",\n  \"paramType\" : {\n    \"exportId\" : \"Text\",\n    \"name\" : \"Text\"\n  },\n  \"question\" : \"Describe the issue\",\n  \"isSavedForTeam\" : false,\n  \"isSavedForUser\" : false\n} ]"}},{"name":"actions","object":{"entries":[{"name":"list-open-pull-requests","object":{"entries":[{"name":"README","object":{"text":""}},{"name":"config.json","object":{"text":"{\n  \"exportId\" : \"DeY65fjDQN6NGM9wdfzmig\",\n  \"name\" : \"list-open-pull-requests\",\n  \"forcePrivateResponse\" : false,\n  \"isDataType\" : false\n}"}},{"name":"function.js","object":{"text":"function(repo, ellipsis) {\n  const GitHubApi = require(\"github\");\nconst github = new GitHubApi();\n\ngithub.authenticate({\n  type: \"oauth\",\n  token: ellipsis.accessTokens.github\n});\n\nconst owner = repo.id.split(\"/\")[0];\nconst repoName = repo.id.split(\"/\")[1];\ngithub.pullRequests.getAll({\n  owner: owner,\n  repo: repoName\n}, function(err, res) {\n  if (err) {\n    ellipsis.error(err.toString());\n  } else {\n    const msg =\n      res.data.length === 0 ?\n        `No open PRs for the **${repo.label}** repo. Go for a walk or something.` :\n        `PRs open at the moment for the **${repo.label}** repo:`;\n    ellipsis.success({ msg: msg, prs: res.data });\n  }\n});\n}\n"}},{"name":"params.json","object":{"text":"[ \"TnqANnwwQZGGcx4GlBRzqw\" ]"}},{"name":"response.md","object":{"text":"{successResult.msg}\n\n{for pr in successResult.prs}\n- [{pr.title}]({pr.html_url}) – @{pr.user.login} – last updated {pr.updated_at}\n{endfor}"}},{"name":"triggers.json","object":{"text":"[ {\n  \"text\" : \"github open PRs\",\n  \"requiresMention\" : false,\n  \"isRegex\" : false,\n  \"caseSensitive\" : false\n}, {\n  \"text\" : \"github open pull requests\",\n  \"requiresMention\" : false,\n  \"isRegex\" : false,\n  \"caseSensitive\" : false\n} ]"}}]}},{"name":"my-issues","object":{"entries":[{"name":"README","object":{"text":"Show open issues assigned to you"}},{"name":"config.json","object":{"text":"{\n  \"exportId\" : \"U6XSsKnhR-GCrCUB9SqXwQ\",\n  \"name\" : \"my-issues\",\n  \"forcePrivateResponse\" : false,\n  \"isDataType\" : false\n}"}},{"name":"function.js","object":{"text":"function(ellipsis) {\n  const groupBy = require('group-by');\nconst GitHubApi = require(\"github\");\nconst github = new GitHubApi();\n\ngithub.authenticate({\n  type: \"oauth\",\n  token: ellipsis.accessTokens.github\n});\n\ngithub.issues.getAll({\n  filter: \"assigned\"\n}, function(err, res) {\n  if (err) {\n    ellipsis.error(err.toString());\n  } else {\n    const grouped = groupBy(res.data, \"repository_url\");\n    const groupedArray = Object.keys(grouped).map(repo => {\n      return { repo: repo, issues: grouped[repo] }\n    });\n    ellipsis.success(groupedArray);\n  }\n});\n}\n"}},{"name":"params.json","object":{"text":"[ ]"}},{"name":"response.md","object":{"text":"{for ea in successResult}\n# Your open issues for: {ea.repo}\n{for issue in ea.issues}\n- [{issue.title}]({issue.html_url})\n{endfor}\n{endfor}"}},{"name":"triggers.json","object":{"text":"[ {\n  \"text\" : \"github list my issues\",\n  \"requiresMention\" : true,\n  \"isRegex\" : false,\n  \"caseSensitive\" : false\n}, {\n  \"text\" : \"github open issues\",\n  \"requiresMention\" : true,\n  \"isRegex\" : false,\n  \"caseSensitive\" : false\n} ]"}}]}},{"name":"new-issue","object":{"entries":[{"name":"README","object":{"text":""}},{"name":"config.json","object":{"text":"{\n  \"exportId\" : \"loAWeTQcS7mVB_CueEjk-Q\",\n  \"name\" : \"new-issue\",\n  \"forcePrivateResponse\" : true,\n  \"isDataType\" : false\n}"}},{"name":"function.js","object":{"text":"function(title, body, repo, ellipsis) {\n  const GitHubApi = require(\"github\");\nconst github = new GitHubApi();\n\ngithub.authenticate({\n  type: \"oauth\",\n  token: ellipsis.accessTokens.github\n});\n\nconst owner = repo.id.split(\"/\")[0];\nconst repoName = repo.id.split(\"/\")[1];\ngithub.issues.create({\n  owner: owner,\n  repo: repoName,\n  title: title,\n  body: body\n}, function(err, res) {\n  if (err) {\n    ellipsis.error(err.toString());\n  } else {\n    ellipsis.success(res.data)\n  }\n});\n}\n"}},{"name":"params.json","object":{"text":"[ \"2c2abbQ9Re-gfyepkhSC3Q\", \"sbTJmKljQButl8kY_5TQCQ\", \"TnqANnwwQZGGcx4GlBRzqw\" ]"}},{"name":"response.md","object":{"text":"New issue created: [{successResult.html_url}]({successResult.html_url})"}},{"name":"triggers.json","object":{"text":"[ {\n  \"text\" : \"github new issue \\\"{title}\\\" \\\"{body}\\\" {repo}\",\n  \"requiresMention\" : false,\n  \"isRegex\" : false,\n  \"caseSensitive\" : false\n}, {\n  \"text\" : \"^github new issue$\",\n  \"requiresMention\" : false,\n  \"isRegex\" : true,\n  \"caseSensitive\" : false\n} ]"}}]}}]}},{"name":"config.json","object":{"text":"{\n  \"name\" : \"GitHub\",\n  \"exportId\" : \"NIi0BJH-Q_StBCsroFh5BQ\",\n  \"icon\" : \"💻\",\n  \"requiredAWSConfigs\" : [ ],\n  \"requiredOAuth2ApiConfigs\" : [ {\n    \"apiId\" : \"lj8s5CF-QnSY8vtbnWj_BA\",\n    \"recommendedScope\" : \"repo\",\n    \"nameInCode\" : \"github\"\n  } ],\n  \"requiredSimpleTokenApis\" : [ ]\n}"}},{"name":"data_type_inputs.json","object":{"text":"[ ]"}},{"name":"data_types","object":{"entries":[{"name":"GithubRepo","object":{"entries":[{"name":"README","object":{"text":""}},{"name":"config.json","object":{"text":"{\n  \"exportId\" : \"vne9aZ6eQ_6hOFwnSm3alg\",\n  \"name\" : \"GithubRepo\",\n  \"forcePrivateResponse\" : false,\n  \"isDataType\" : true,\n  \"dataTypeConfig\" : {\n    \"fields\" : [ ],\n    \"usesCode\" : true\n  }\n}"}},{"name":"function.js","object":{"text":"function(ellipsis) {\n  const GitHubApi = require(\"github\");\nconst github = new GitHubApi();\n\ngithub.authenticate({\n  type: \"oauth\",\n  token: ellipsis.accessTokens.github\n});\n\ngithub.repos.getAll({}, function(err, res) {\n  if (err) {\n    ellipsis.error(err.toString());\n  } else {\n    ellipsis.success(res.data.map((ea) => {\n      return { id: ea.full_name, label: ea.full_name };\n    }));\n  }\n});\n}\n"}},{"name":"params.json","object":{"text":"[ ]"}},{"name":"response.md","object":{"text":""}},{"name":"triggers.json","object":{"text":"[ ]"}}]}}]}}]}""")
      when(dataService.teamEnvironmentVariables.lookForInCode(anyString)).thenReturn(Seq())
      val data = GithubBehaviorGroupDataBuilder(json, team, "ellipsis-ai", "github", None, None, dataService).build
      data.name mustBe Some("GitHub")
      data.description mustBe Some("Quickly view and create issues, pull requests and more")
      data.actionBehaviorVersions must have length(3)
      data.dataTypeBehaviorVersions must have length(1)
      data.actionInputs must have length(3)
      data.dataTypeInputs must have length(0)
      data.requiredOAuth2ApiConfigs must have length(1)
    }

    "build BehaviorGroupData from JSON, including a library" in new TestContext {
      val json: JsValue = Json.parse("""{"name":"AdRoll","entries":[{"name":"README","object":{"text":"A Skill to fetch data from the AdRoll API"}},{"name":"action_inputs.json","object":{"text":"[ {\n  \"exportId\" : \"7_szWGZXRda6UwoCnl1HNQ\",\n  \"name\" : \"dateRange\",\n  \"paramType\" : {\n    \"exportId\" : \"Text\",\n    \"name\" : \"Text\"\n  },\n  \"question\" : \"What time range are you interested in?\",\n  \"isSavedForTeam\" : false,\n  \"isSavedForUser\" : false\n}, {\n  \"exportId\" : \"DtEGd6g4RtuijF6SEugFEw\",\n  \"name\" : \"advertisableEID\",\n  \"paramType\" : {\n    \"exportId\" : \"Text\",\n    \"name\" : \"Text\"\n  },\n  \"question\" : \"What is the Advertisable EID?\",\n  \"isSavedForTeam\" : false,\n  \"isSavedForUser\" : false\n}, {\n  \"exportId\" : \"VKijhO0WT4GHnIKMpC_zEw\",\n  \"name\" : \"organizationEID\",\n  \"paramType\" : {\n    \"exportId\" : \"Text\",\n    \"name\" : \"Text\"\n  },\n  \"question\" : \"What is the value for `organizationEID`?\",\n  \"isSavedForTeam\" : false,\n  \"isSavedForUser\" : false\n} ]"}},{"name":"actions","object":{"entries":[{"name":"get_my_org_info","object":{"entries":[{"name":"README","object":{"text":""}},{"name":"config.json","object":{"text":"{\n  \"exportId\" : \"xC4du0AZRauT213o0ig8zg\",\n  \"name\" : \"get_my_org_info\",\n  \"forcePrivateResponse\" : false,\n  \"isDataType\" : false\n}"}},{"name":"function.js","object":{"text":"function(ellipsis) {\n  const prettyjson = require('prettyjson');\nconst AdRollHelper = require('./adroll');\nconst adRoll = new AdRollHelper(ellipsis);\n\nadRoll.validateAPIisReacheable()\n.then(() => {\n  return adRoll.getOrgInfo({organizationEID: null});\n})\n.then((orgInfo) => {\n  ellipsis.success(prettyjson.render(orgInfo, {}));\n});\n}\n"}},{"name":"params.json","object":{"text":"[ ]"}},{"name":"response.md","object":{"text":"```\n{successResult}\n```"}},{"name":"triggers.json","object":{"text":"[ {\n  \"text\" : \"get my org info\",\n  \"requiresMention\" : true,\n  \"isRegex\" : false,\n  \"caseSensitive\" : false\n} ]"}}]}},{"name":"get_organization_info","object":{"entries":[{"name":"README","object":{"text":""}},{"name":"config.json","object":{"text":"{\n  \"exportId\" : \"KCUOis7gS6eGbwdkFexNLg\",\n  \"name\" : \"get_organization_info\",\n  \"forcePrivateResponse\" : false,\n  \"isDataType\" : false\n}"}},{"name":"function.js","object":{"text":"function(organizationEID, ellipsis) {\n  const prettyjson = require('prettyjson');\nconst AdRollHelper = require('./adroll');\nconst adRoll = new AdRollHelper(ellipsis);\n\n\nadRoll.validateAPIisReacheable()\n.then(() => {\n  return adRoll.getOrgInfo({organizationEID: organizationEID});\n})\n.then((orgInfo) => {\n  ellipsis.success(prettyjson.render(orgInfo, {}));\n});\n}\n"}},{"name":"params.json","object":{"text":"[ \"VKijhO0WT4GHnIKMpC_zEw\" ]"}},{"name":"response.md","object":{"text":"```\n{successResult}\n```"}},{"name":"triggers.json","object":{"text":"[ {\n  \"text\" : \"get org info\",\n  \"requiresMention\" : true,\n  \"isRegex\" : false,\n  \"caseSensitive\" : false\n}, {\n  \"text\" : \"get org info for {organizationEID}\",\n  \"requiresMention\" : true,\n  \"isRegex\" : false,\n  \"caseSensitive\" : false\n} ]"}}]}},{"name":"get_report","object":{"entries":[{"name":"README","object":{"text":""}},{"name":"config.json","object":{"text":"{\n  \"exportId\" : \"xK1RI0TGSZu46CdKZdcnxg\",\n  \"name\" : \"get_report\",\n  \"forcePrivateResponse\" : false,\n  \"isDataType\" : false\n}"}},{"name":"function.js","object":{"text":"function(advertisableEID, dateRange, ellipsis) {\n  const AdRollHelper = require('./adroll');\nconst adRoll = new AdRollHelper(ellipsis);\nconst dateRangeParser = require('ellipsis-date-range-parser');\n\nconst parsedRange = dateRangeParser.parse(dateRange, ellipsis.teamInfo.timeZone);\n\nconst inputs = {\n  advertisableEID: advertisableEID,\n  dateRange: parsedRange\n};\n\nadRoll.validateAPIisReacheable()\n  .then(() => adRoll.validateAdvertisableEID(advertisableEID))\n  .then(() => adRoll.validateDateRange(parsedRange, dateRange))\n  .then(() => adRoll.getReportRecords(inputs))\n  .then((result) => {\n  \n    // Creates a CSV file to send to the user\n    \n    const reportType = \"AM\";\n    const start = inputs.dateRange.start.toISOString().slice(0,10);\n    const end = inputs.dateRange.end.toISOString().slice(0,10);\n    if (result.records.length > 0 ){\n       const filename = [\"report\", reportType, start, end].join(\"_\") + \".csv\";\n       const files = [{\n         content: result.records.join('\\n'),\n         filetype: \"csv\",\n         filename: filename\n        }];\n        ellipsis.success(`I  have found ${result.records.length} records from ${start} to ${end}`, { files: files });\n    } else {\n      ellipsis.success(`I did not get back any records for EID ${advertisableEID} between ${start} and ${end}`); \n    }\n});\n}\n"}},{"name":"params.json","object":{"text":"[ \"DtEGd6g4RtuijF6SEugFEw\", \"7_szWGZXRda6UwoCnl1HNQ\" ]"}},{"name":"response.md","object":{"text":"{successResult}"}},{"name":"triggers.json","object":{"text":"[ {\n  \"text\" : \"get report\",\n  \"requiresMention\" : true,\n  \"isRegex\" : false,\n  \"caseSensitive\" : false\n}, {\n  \"text\" : \"get report {advertisableEID} for {dateRange}\",\n  \"requiresMention\" : true,\n  \"isRegex\" : false,\n  \"caseSensitive\" : false\n}, {\n  \"text\" : \"get report {advertisableEID} from {dateRange}\",\n  \"requiresMention\" : true,\n  \"isRegex\" : false,\n  \"caseSensitive\" : false\n} ]"}}]}},{"name":"help","object":{"entries":[{"name":"README","object":{"text":""}},{"name":"config.json","object":{"text":"{\n  \"exportId\" : \"-HUrcbq3QpacbsIe2nEndQ\",\n  \"name\" : \"help\",\n  \"forcePrivateResponse\" : false,\n  \"isDataType\" : false\n}"}},{"name":"function.js","object":{"text":"function(ellipsis) {\n  \n}\n"}},{"name":"params.json","object":{"text":"[ ]"}},{"name":"response.md","object":{"text":"The AdRoll Skill helps you get data about Organizations and Advertisable by quering the AdRoll APIs for you.    \n\n**Can you show me some examples**\n```\n@ellipsis get my org info\n@ellipsis get org info for <Org EID>\n@ellipsis get report\n@ellispis get report <AdvertisableEID> for last month\n@ellispis get report <AdvertisableEID> from 4/1/2017 to 5/1/2017\n```\n\n**What kind of report or data can I get?**  \n```\nYou can get data about an Organization or an AM report.\n```\n**How do I specify the date range?** \n```\nValid date ranges formats and examples:\n - '11/25/2017 - 11/30/2017'\n - 'from April 20 to May 1'\n - 'this week', 'this month', 'this year'\n - 'week to date', 'month to date', 'year to date'\n - 'last week', 'last month', 'last year'\n - 'week to date' or 'wtd', 'month to date' or 'mtd', 'year to date' or 'ytd'\n ```"}},{"name":"triggers.json","object":{"text":"[ {\n  \"text\" : \"adroll help\",\n  \"requiresMention\" : true,\n  \"isRegex\" : false,\n  \"caseSensitive\" : false\n} ]"}}]}}]}},{"name":"config.json","object":{"text":"{\n  \"name\" : \"AdRoll\",\n  \"exportId\" : \"AscmjM2uSCO3RsZLocaz-A\",\n  \"icon\" : \"🌭\",\n  \"requiredAWSConfigs\" : [ ],\n  \"requiredOAuth2ApiConfigs\" : [ {\n    \"apiId\" : \"AU3mdlpMSoWtqI8BiBdITw\",\n    \"recommendedScope\" : \"all\",\n    \"nameInCode\" : \"adRollApp\"\n  } ],\n  \"requiredSimpleTokenApis\" : [ ]\n}"}},{"name":"data_type_inputs.json","object":{"text":"[ {\n  \"exportId\" : \"Uc50xXH4RtGkCEGppKa_gQ\",\n  \"name\" : \"searchQuery\",\n  \"paramType\" : {\n    \"exportId\" : \"Text\",\n    \"name\" : \"Text\"\n  },\n  \"question\" : \"What is the value for `searchQuery`?\",\n  \"isSavedForTeam\" : false,\n  \"isSavedForUser\" : false\n} ]"}},{"name":"data_types","object":{"entries":[{"name":"AdvertisableEID","object":{"entries":[{"name":"README","object":{"text":""}},{"name":"config.json","object":{"text":"{\n  \"exportId\" : \"Iu9ApncBRpSO1VxdYDI7oQ\",\n  \"name\" : \"AdvertisableEID\",\n  \"forcePrivateResponse\" : false,\n  \"isDataType\" : true,\n  \"dataTypeConfig\" : {\n    \"fields\" : [ ],\n    \"usesCode\" : true\n  }\n}"}},{"name":"function.js","object":{"text":"function(searchQuery, ellipsis) {\n  const AdRollHelper = require('./adroll');\nconst adRoll = new AdRollHelper(ellipsis);\n\nadRoll.validateAPIisReacheable()\n  .then(() => adRoll.validateAdvertisableEID(searchQuery.trim()))\n  .then(() => ellipsis.success([ { label: searchQuery, id: searchQuery } ]));\n}\n"}},{"name":"params.json","object":{"text":"[ \"Uc50xXH4RtGkCEGppKa_gQ\" ]"}},{"name":"response.md","object":{"text":""}},{"name":"triggers.json","object":{"text":"[ ]"}}]}}]}},{"name":"lib","object":{"entries":[{"name":"adroll.js","object":{"text":"/*\nSimple methods to handle interaction with the AdRoll GraphQL API\n@exportId W1PqDGz3Sca22A5aCX1mow\n*/\nmodule.exports = (function() {\nconst graphqlRequest = require('graphql-request');\n\n// Takes a list of AdRoll Campaign objects and create an\n// array of csv records.\nfunction extractAMReportRecords(campaigns) {\n  var records = [];\n  campaigns.forEach((c) => {\n    c.adgroups.forEach((adGroup) => {\n      adGroup.ads.forEach((ad) => {\n        ad.metrics.byDate.forEach((m) => {\n          records.push(\n            [\n              m.date,\n              (new Date(m.date)).toLocaleString('en-us', {weekday: 'long'}),\n              c.type || \"\",\n              c.channel || \"\",\n              c.name,\n              adGroup.name,\n              ad.status,\n              ad.name,\n              ad.width + \"x\" + ad.height,\n              ad.type,\n              m.cost,\n              m.impressions,\n              m.clicks,\n              m.viewThroughs || \"\",\n              m.clickThroughs || \"\",\n              m.viewRevenue || \"\",\n              m.clickRevenue || \"\"\n            ].join()\n          );\n        });\n      });\n    });\n  });\n  return records;\n}\n\nclass Adroll {\n  \n  constructor(ellipsis) {\n    this.ellipsis = ellipsis;\n    this.graphQL = this.buildGraphQLClient(this.ellipsis.accessTokens.adRollApp);\n  }\n  \n  handleAPIError(response, options={}) {\n    var whileDoing= options.whileDoing || \"talking to the AdRoll API.\";\n    var message= options.message || \"The API returned\";\n    var userMessage = `Something went wrong while ${whileDoing}`;\n    var detailedMessage = message;\n    \n    if (response.name === 'FetchError') {\n      detailedMessage = response.message;\n    } else if (response.status == 200) {\n      console.log(\"GraphQL error!\");\n      console.log(JSON.stringify(response));\n      detailedMessage = options.message || \"Something went wrong while quering the GraphQL API. Check your queries.\";\n      detailedMessage = detailedMessage + `\\n GraphQL error: ${response.response.errors[0].message}`;\n    } else {\n      const errorMessage = (response.errors && response.errors.length) ? response.errors[0].message: \"No details provided\";\n      detailedMessage = `${message} ${response.status}: ${errorMessage}`;\n    }\n    userMessage = `${userMessage}\\n\nMore details:  \n\\`\\`\\`\n${detailedMessage}\n\\`\\`\\`\n`\n    throw new this.ellipsis.Error(detailedMessage, {userMessage: userMessage})\n  }\n\n  buildGraphQLClient(oauth2Token) {\n    const endpoint  = 'https://services.adroll.com/reporting/api/v1/query';\n    const authHeaderValue = \"Bearer \" + oauth2Token;\n    return new graphqlRequest.GraphQLClient(endpoint, {\n      headers: { Authorization: authHeaderValue }\n    });\n  }\n\n  validateAPIisReacheable() {\n    const query = `query { organization { current { name } } }`;\n    return this.graphQL.request(query)\n      .catch((response) => {\n        this.handleAPIError(response.response);\n      });\n  }\n\n  validateAdvertisableEID(advertisableEID) {\n    const query = `{\n      advertisable {\n        byEID(advertisable: $advertisableEID) {\n          organization\n        }\n      }\n    }`;\n    const variables = {\n      advertisableEID: advertisableEID\n    };\n    return this.graphQL.request(query, variables)\n      .catch((response) => {\n        this.handleAPIError(response.response, {\n          whileDoing: \"validating the EID\", \n          message: `The EID ${advertisableEID} is invalid.`\n        });\n      });\n  }\n\n  validateDateRange(dateRange, userInput) {\n    return new Promise((resolve, reject) => {\n      if (!dateRange) {\n        reject(new this.ellipsis.Error(`The date range \"${userInput}\" is invalid`, { userMessage: \"User will see this\", errors:  [\"A valid date range cannot be parsed.\"], errorType: \"DATE_RANGE_ERROR\"}));\n      }\n\n      var errors = [];\n      if (!dateRange.start) {\n        errors.push(\"start date is not defined\");\n      }\n      if (!dateRange.end){\n        errors.push(\"end date is not defined\");\n      }\n      if (dateRange.start && dateRange.end && (dateRange.start > dateRange.end) ) {\n        errors.push(\"Start date cannot be greater then end date\");\n      }\n      if (errors.length > 0) {\n        reject(new this.ellipsis.Error(`The date range \"${userInput}\" is invalid`, { userMessage: \"User will see this\", errors:  [\"A valid date range cannot be parsed.\"], errorType: \"DATE_RANGE_ERROR\"}));\n      }\n\n      resolve();\n    });\n  }\n\n  getReportRecords(inputs) {\n    const csvHeaders = [\n      \"date\",\n      \"day_of_week\",\n      \"product\",\n      \"inventory_source\",\n      \"campaign\",\n      \"adgroup\",\n      \"status\",\n      \"ad\",\n      \"ad_size\",\n      \"type\",\n      \"cost\",\n      \"impressions\",\n      \"clicks\",\n      \"adjusted_total_conversions\",\n      \"adjusted_ctc\",\n      \"adjusted_vtc\",\n      \"attributed_rev\",\n      \"attributed_click_through_rev\",\n      \"attributed_view_through_rev\"\n    ].join(\",\");\n\n    const query = `{\n      advertisable {\n        byEID(advertisable: $advertisableEID) {\n          eid\n          name\n          campaigns {\n            name\n            adgroups {\n              name\n              ads {\n                name\n                status\n                type\n                height\n                width\n                adFormatName\n                metrics(start: $startDate, end: $endDate, currency: \"USD\") {\n                  byDate {\n                    impressions\n                    clicks\n                    cost\n                    viewThroughs\n                    clickThroughs\n                    viewRevenue\n                    clickRevenue\n                    date\n                  }\n                }\n              }\n            }\n          }\n        }\n      }\n    }`;\n    const variables = {\n      advertisableEID: inputs.advertisableEID,\n      startDate: inputs.dateRange.start.toISOString().slice(0,10),\n      endDate: inputs.dateRange.end.toISOString().slice(0,10)\n    };\n\n    return this.graphQL.request(query, variables)\n      .then((data) => {\n         return {\n           records: [csvHeaders].concat(extractAMReportRecords(data.advertisable.byEID.campaigns))\n         };\n      })\n      .catch((response) => {\n        this.handleAPIError(response.response, {whileDoing: \"fetching the AM Report records\"});\n      });\n  }\n\n  getOrgInfo(inputs) {\n    var byWhat = \"current\";\n    var variables = {};\n    if (inputs.organizationEID) {\n      byWhat = `byEID(organization: $organizationEID)`;\n      variables = {organizationEID: inputs.organizationEID};\n    }\n    var query = `query { \n      organization { \n        ${byWhat} {  \n          name\n          eid\n          createdDate\n          advertisables {\n            eid\n            name\n          }\n          campaigns(isActive: true) {\n            eid\n            name\n          }  \n        }\n       }\n    }`;\n    return this.graphQL.request(query, variables)\n               .then(data => data.organization.byEID || data.organization.current)\n               .catch(response => {\n                 this.handleAPIError(response.response, {whileDoing: \"fetching the Organization info\"});\n               });\n  }\n}\n\nreturn Adroll;\n\n})()\n     "}}]}}]}""")
      when(dataService.teamEnvironmentVariables.lookForInCode(anyString)).thenReturn(Seq())
      val data = GithubBehaviorGroupDataBuilder(json, team, "ellipsis-ai", "adroll", None, None, dataService).build
      data.libraryVersions must have length(1)
      data.libraryVersions.head.name mustBe "adroll"
    }

  }

}
