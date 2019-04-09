import BehaviorGroup, {BehaviorGroupJson} from '../../../../app/assets/frontend/models/behavior_group';
import {
  TextPart,
  MultiLineTextPropertyDiff,
  maybeDiffFor,
  ModifiedDiff
} from '../../../../app/assets/frontend/models/diffs';
import {BehaviorVersionJson} from "../../../../app/assets/frontend/models/behavior_version";
import {LibraryVersionJson} from "../../../../app/assets/frontend/models/library_version";
import {InputJson} from "../../../../app/assets/frontend/models/input";
import {RequiredAWSConfigJson} from "../../../../app/assets/frontend/models/aws";
import {RequiredOAuthApplication, RequiredOAuthApplicationJson} from "../../../../app/assets/frontend/models/oauth";
import {RequiredSimpleTokenApiJson} from "../../../../app/assets/frontend/models/simple_token";
import {TriggerType} from "../../../../app/assets/frontend/models/trigger";

const teamId = 'team123456';
const groupId = 'group123456';
const behaviorId = 'ghijkl';
const libraryId = 'lib123456';
const inputId = 'input123456';
const inputId2 = 'input234567';
const requiredAWSConfigId = 'requiredAWS123456';
const requiredTrelloConfigId = 'requiredTrello123456';
const requiredGithubConfigId = 'requiredGithub123456';
const requiredPivotalTrackerConfigId = 'requiredPivotalTracker123456';

const normalResponseType = "Normal";

const privateResponseType = "Private";

const behaviorVersion1: BehaviorVersionJson = {
  "id": "abcdef",
  teamId: "1",
  "name": "First name",
  description: null,
  exportId: "abcdef",
  "groupId": groupId,
  "behaviorId": behaviorId,
  "functionBody": "use strict;",
  "responseTemplate": "A template",
  "triggers": [{
    "text": "B",
    "requiresMention": false,
    "isRegex": true,
    "caseSensitive": false,
    "triggerType": TriggerType.MessageSent
  }, {
    "text": "C",
    "requiresMention": false,
    "isRegex": false,
    "caseSensitive": false,
    "triggerType": TriggerType.MessageSent
  }],
  "inputIds": [inputId, inputId2],
  "config": {
    responseTypeId: normalResponseType,
    isDataType: false,
    isTest: false,
    exportId: null,
    name: null,
    dataTypeConfig: null
  },
  "createdAt": 1468338136532,
  isNew: false
};
const behaviorVersion2: BehaviorVersionJson = {
  "id": "abcdef",
  teamId: "1",
  "name": "Second name",
  "description": "A description",
  exportId: "abcdef",
  "groupId": groupId,
  "behaviorId": behaviorId,
  "functionBody": "use strict; // so strict",
  "responseTemplate": "Another template",
  "triggers": [{
    "text": "B",
    "requiresMention": true,
    "isRegex": true,
    "caseSensitive": false,
    "triggerType": TriggerType.MessageSent
  }, {
    "text": ".+",
    "requiresMention": false,
    "isRegex": true,
    "caseSensitive": false,
    "triggerType": TriggerType.MessageSent
  }],
  "inputIds": [inputId2, inputId],
  "config": {
    responseTypeId: privateResponseType,
    isDataType: false,
    isTest: false,
    exportId: null,
    name: null,
    dataTypeConfig: null
  },
  "createdAt": 1468359271138,
  isNew: false
};

const libraryVersion1: LibraryVersionJson = {
  id: 'abcdef',
  name: 'some-lib',
  description: 'A library',
  functionBody: 'return "foo"',
  groupId: groupId,
  teamId: teamId,
  libraryId: libraryId,
  isNew: false,
  exportId: 'abcdef',
  createdAt: Date.now()
};

const libraryVersion2: LibraryVersionJson = {
  id: 'abcdef',
  name: 'some-lib-revised',
  description: 'A library (revised)',
  functionBody: 'return "foo";',
  groupId: groupId,
  teamId: teamId,
  libraryId: libraryId,
  isNew: false,
  exportId: 'abcdef',
  createdAt: Date.now()
};

const actionInput1: InputJson = {
  name: 'clown',
  question: 'what drives the car?',
  paramType: {
    id: 'Text',
    name: 'Text',
    exportId: 'Text',
    needsConfig: false,
    typescriptType: 'string'
  },
  isSavedForTeam: false,
  isSavedForUser: true,
  inputId: inputId,
  exportId: inputId
};

const actionInputChanged: InputJson = {
  name: 'clown',
  question: 'who drives the car?',
  paramType: {
    id: 'sdflkjafks',
    name: 'Person',
    exportId: 'sdflkjafks',
    needsConfig: false,
    typescriptType: '{ id: string, label: string, [k: string]: any }'
  },
  isSavedForTeam: true,
  isSavedForUser: false,
  inputId: inputId,
  exportId: inputId
};

const actionInput2: InputJson = {
  name: 'somethingElse',
  question: 'and now for something?',
  paramType: {
    id: 'Text',
    name: 'Text',
    exportId: 'Text',
    needsConfig: false,
    typescriptType: 'string'
  },
  isSavedForTeam: false,
  isSavedForUser: false,
  inputId: inputId2,
  exportId: inputId2
};

const requiredAWSConfig1: RequiredAWSConfigJson = {
  id: 'aws123',
  exportId: requiredAWSConfigId,
  apiId: 'aws',
  nameInCode: 'prod',
  config: null
};

const requiredAWSConfig2: RequiredAWSConfigJson = {
  id: 'aws123',
  exportId: requiredAWSConfigId,
  apiId: 'aws',
  nameInCode: 'prod',
  config: {
    id: 'aws-prod',
    displayName: 'AWS Prod'
  }
};

const requiredOAuth1Config1: RequiredOAuthApplicationJson = {
  id: 'trello123',
  exportId: requiredTrelloConfigId,
  apiId: 'trello',
  nameInCode: 'trello',
  config: null,
  recommendedScope: 'read'
};

const requiredOAuth2Config1: RequiredOAuthApplicationJson = {
  id: 'github123',
  exportId: requiredGithubConfigId,
  apiId: 'github',
  nameInCode: 'github',
  config: null,
  recommendedScope: 'repo'
};

const requiredOAuth2Config2: RequiredOAuthApplicationJson = {
  id: 'github123',
  exportId: requiredGithubConfigId,
  apiId: 'github',
  nameInCode: 'githubReadonly',
  config: null,
  recommendedScope: 'repo:readonly'
};

const requiredOAuth2Config3: RequiredOAuthApplicationJson = {
  id: 'github12345',
  exportId: 'requiredGithubabcdef',
  apiId: 'github',
  nameInCode: 'githubReadwrite',
  config: null,
  recommendedScope: 'repo'
};

const requiredSimpleTokenApi1: RequiredSimpleTokenApiJson = {
  id: 'pivotalTracker123',
  exportId: requiredPivotalTrackerConfigId,
  apiId: 'pivotalTracker',
  nameInCode: 'pivotalTracker',
};

const requiredSimpleTokenApi2: RequiredSimpleTokenApiJson = {
  id: 'pivotalTracker123',
  exportId: requiredPivotalTrackerConfigId,
  apiId: 'pivotalTracker',
  nameInCode: 'pivotalTracker2',
};

const behaviorGroupVersion1: BehaviorGroupJson = {
  id: "1",
  teamId: "1",
  createdAt: Date.now(),
  name: "Some skill",
  icon: "🚀",
  description: null,
  behaviorVersions: [behaviorVersion1],
  requiredAWSConfigs: [requiredAWSConfig1],
  requiredOAuthApiConfigs: [requiredOAuth1Config1, requiredOAuth2Config1, requiredOAuth2Config3],
  requiredSimpleTokenApis: [requiredSimpleTokenApi1],
  actionInputs: [actionInput1, actionInput2],
  dataTypeInputs: [],
  libraryVersions: [libraryVersion1],
  exportId: null,
  author: null,
  gitSHA: null,
  deployment: null,
  metaData: null,
  isManaged: false
};

const behaviorGroupVersion2: BehaviorGroupJson = {
  id: "1",
  teamId: "1",
  createdAt: Date.now(),
  name: "Some updated skill",
  icon: null,
  description: "With a description",
  behaviorVersions: [behaviorVersion2],
  requiredAWSConfigs: [requiredAWSConfig2],
  requiredOAuthApiConfigs: [requiredOAuth1Config1, requiredOAuth2Config2, requiredOAuth2Config3],
  requiredSimpleTokenApis: [requiredSimpleTokenApi2],
  actionInputs: [actionInputChanged, actionInput2],
  dataTypeInputs: [],
  libraryVersions: [libraryVersion2],
  exportId: null,
  author: null,
  gitSHA: null,
  deployment: null,
  metaData: null,
  isManaged: false
};

const publishedMyCalendar: BehaviorGroup = BehaviorGroup.fromJson({
  "teamId": "v-i65oxZQDiBsZuXceONmA",
  "name": "My Calendar",
  "description": "Helps you keep track of your personal calendar by sending you a daily agenda and sending reminders before each event throughout the day. Say “setup my calendar” in a direct message to set it up. Or type “my calendar help” for more info.",
  "icon": "⏰",
  "actionInputs": [{
    "id": "4a_489bfTRusDpqTG-LzRQ",
    "inputId": "fLL6qZ9rSR-gdVXrbz8sEQ",
    "exportId": "_lpHYaeKQfqd4Gghk3fhGg",
    "name": "whenToAnnounce",
    "paramType": {"exportId": "Text", "name": "Text", typescriptType: "string"},
    "question": "What time should I send you your agenda for the day? e.g. “9 AM” or “10 AM Eastern time”, or “none” if you don’t want an agenda",
    "isSavedForTeam": false,
    "isSavedForUser": false
  }, {
    "id": "EluEPwSxSKal_VX_x2X9iA",
    "inputId": "P1GZEO9nTKKFl5dP707j1A",
    "exportId": "w4C6zIa4SNO1X_91WiQTTQ",
    "name": "shouldRemind",
    "paramType": {"exportId": "Yes/No", "name": "Yes/No", typescriptType: "boolean"},
    "question": "Would you like me to send you a reminder before each event begins?",
    "isSavedForTeam": false,
    "isSavedForUser": false
  }],
  "dataTypeInputs": [],
  "behaviorVersions": [{
    "id": "7ijU4AHjTpeP9W7_arxSZQ",
    "teamId": "v-i65oxZQDiBsZuXceONmA",
    "behaviorId": "VuKRcP6mTd2S7I7PZ2nX-w",
    "isNew": false,
    "name": "Agenda",
    "description": "",
    "functionBody": "const moment = require('moment-timezone');\nconst gcal = require('google-calendar');\nconst cal = new gcal.GoogleCalendar(ellipsis.accessTokens.googleCalendar);\nconst Formatter = ellipsis.require('ellipsis-cal-date-format@0.0.13');\nconst eventlib = require('eventlib');\n\ncal.calendars.get(\"primary\", (err, res) => {\n  if (err) {\n    throw new ellipsis.Error(`An error occurred retrieving your primary calendar (${err.code}): ${err.message}`, {\n      userMessage: \"An error occurred while fetching your calendar from Google. You may try running `...what's on my calendar today` again to see if it was temporary.\"\n    });\n  } else {\n    const tz = res.timeZone;\n    list(tz || ellipsis.userInfo.timeZone || ellipsis.teamInfo.timeZone);\n  }\n});\n\nfunction list(tz) {\n  moment.tz.setDefault(tz);\n  const now = moment();\n  const min = now.clone();\n  const max = now.clone().startOf('day').add(1, 'days');\n  cal.events.list(\"primary\", {\n    timeMin: min.toISOString(),\n    timeMax: max.toISOString(),\n    orderBy: 'startTime',\n    singleEvents: true\n  }, (err, res) => {\n    if (err) {\n      ellipsis.error(`An error occurred fetching your calendar. (${err.code}: ${err.message})`);\n    } else if (!res.items) {\n      ellipsis.error(\"There was a problem fetching your calendar. Google Calendar may be experiencing a hiccup.\");\n    } else {\n      const items = eventlib.filterDeclined(res.items.slice());\n      let heading = \"\";\n      if (items.length === 0) {\n        heading = \"🎉 There’s nothing on your calendar for the rest of the day.\";\n      } else if (items.length === 1) {\n        heading = \"There’s 1 event on your calendar today:\";\n      } else {\n        heading = `There are ${items.length} events on your calendar today:`;\n      }\n      const result = {\n        heading: heading,\n        items: items.map((event) => {\n          return Object.assign({}, event, {\n            formattedEvent: Formatter.formatEvent(event, tz, now.format(Formatter.formats.YMD))\n          });\n        })\n      };\n      ellipsis.success(result);\n    }\n  });\n}",
    "responseTemplate": "**{successResult.heading}**\n{for event in successResult.items}\n{event.formattedEvent}\n{endfor}\n",
    "inputIds": [],
    "triggers": [{
      "text": "what's on my calendar today",
      "requiresMention": true,
      "isRegex": false,
      "caseSensitive": false,
      "triggerType": TriggerType.MessageSent
    }],
    "config": {
      "exportId": "BXUYJotxSaKz3QqZ_zSd-w",
      "name": "Agenda",
      "responseTypeId": normalResponseType,
      "isDataType": false,
      "isTest": false
    },
    "exportId": "BXUYJotxSaKz3QqZ_zSd-w",
  }, {
    "id": "BlFk7azKRjaCgH5ricykTA",
    "teamId": "v-i65oxZQDiBsZuXceONmA",
    "behaviorId": "54TsRxxtQEeko-KR7Kn26g",
    "isNew": false,
    "name": "Deactivate",
    "description": "",
    "functionBody": "const EllipsisApi = ellipsis.require('ellipsis-api');\nconst api = new EllipsisApi(ellipsis).actions;\napi.unschedule({\n  actionName: \"Agenda\",\n  channel: ellipsis.userInfo.messageInfo.channel,\n  userId: ellipsis.userInfo.ellipsisUserId\n}).then(() => {\n  return api.unschedule({\n    actionName: \"Reminders\",\n    channel: ellipsis.userInfo.messageInfo.channel,\n    userId: ellipsis.userInfo.ellipsisUserId\n  });\n}).then(() => {\n  ellipsis.success();\n}).catch((err) => {\n  throw new ellipsis.Error(err, { userMessage: \"I tried to turn off your agenda and calendar reminders, but something went wrong. Try again, or else try unscheduling it manually.\" });\n});",
    "responseTemplate": "OK. I will no longer send you your agenda or reminders in this channel.\n",
    "inputIds": [],
    "triggers": [{
      "text": "deactivate my calendar",
      "requiresMention": true,
      "isRegex": false,
      "caseSensitive": false,
      "triggerType": TriggerType.MessageSent
    }, {
      "text": "stop my calendar",
      "requiresMention": true,
      "isRegex": false,
      "caseSensitive": false,
      "triggerType": TriggerType.MessageSent
    }, {"text": "turn off my calendar", "requiresMention": true, "isRegex": false, "caseSensitive": false, "triggerType": TriggerType.MessageSent}],
    "config": {
      "exportId": "V-LAjv1AS4CoFimMANnxpg",
      "name": "Deactivate",
      "responseTypeId": normalResponseType,
      "isDataType": false,
      "isTest": false
    },
    "exportId": "V-LAjv1AS4CoFimMANnxpg",
  }, {
    "id": "AFV_L2J3Sp2DRxx4t9Qdjw",
    "teamId": "v-i65oxZQDiBsZuXceONmA",
    "behaviorId": "ZzzHEhz5RlmaxPAiQKT8Qg",
    "isNew": false,
    "name": "Help",
    "description": "",
    "functionBody": "",
    "responseTemplate": "Ellipsis can show you what's happening on your calendar today, and send reminders when events are about to begin. (This skill requires access to your Google Calendar.)\n\n**Actions:**\n- `what's on my calendar today` — show your agenda (list all events) for the rest of the day\n- `what's on my calendar now` — show any events happening now or in the next 10 minutes\n- `setup my calendar` — set up the skill to send you your agenda each weekday, and send you reminders a few minutes before events begin\n- `stop my calendar` — turn off the scheduled agenda and reminders\n",
    "inputIds": [],
    "triggers": [{"text": "my calendar help", "requiresMention": true, "isRegex": false, "caseSensitive": false, "triggerType": TriggerType.MessageSent}],
    "config": {
      "exportId": "J0uB9LvZTo6_L-spEBrtqg",
      "name": "Help",
      "responseTypeId": normalResponseType,
      "isDataType": false,
      "isTest": false
    },
    "exportId": "J0uB9LvZTo6_L-spEBrtqg",
  }, {
    "id": "Eb6wNX-WS1KiobO6Khud6w",
    "teamId": "v-i65oxZQDiBsZuXceONmA",
    "behaviorId": "vyfZP4t0SGyZZlSQ9nS55Q",
    "isNew": false,
    "name": "Reminders",
    "description": "",
    "functionBody": "const moment = require('moment-timezone');\nmoment.tz.setDefault(ellipsis.userInfo.timeZone || ellipsis.teamInfo.timeZone);\nconst gcal = require('google-calendar');\nconst cal = new gcal.GoogleCalendar(ellipsis.accessTokens.googleCalendar);\nconst Formatter = ellipsis.require('ellipsis-cal-date-format@0.0.13');\nconst eventlib = require('eventlib');\n\nlist();\n\nfunction list() {\n  const now = moment();\n  const min = now.clone();\n  const max = now.clone().add(8, 'minutes');\n  cal.events.list(\"primary\", {\n    timeMin: min.toISOString(),\n    timeMax: max.toISOString(),\n    orderBy: 'startTime',\n    singleEvents: true\n  }, (err, res) => {\n    const errorMessage = \"An error occurred while checking your Google calendar for upcoming events. You may try running `...what's on my calendar now` to see if it happens again. The problem may be temporary.\";\n    if (err) {\n      throw new ellipsis.Error(`Error ${err.code}: ${err.message}`, {\n        userMessage: errorMessage\n      });\n    } else if (!res.items) {\n      throw new ellipsis.Error(\"Google Calendar returned an invalid response (no items).\", {\n        userMessage: errorMessage\n      });\n    } else {\n      const tz = res.timeZone || ellipsis.teamInfo.timeZone;\n      moment.tz.setDefault(tz);\n      const items = eventlib.filterDeclined(res.items.filter((ea) => {\n        return moment(ea.start.dateTime).isAfter(now.clone().add(2, 'minutes').add(30, 'seconds'))\n      }));\n      if (items.length === 0) {\n        if (ellipsis.event.originalEventType === \"scheduled\") {\n          ellipsis.noResponse();\n        } else {\n          ellipsis.success({\n            hasItems: false\n          });\n        }\n      } else {\n        ellipsis.success({\n          hasItems: true,\n          heading: items.length > 1 ?\n            `Reminder: there are ${items.length} events on your calendar.` :\n            `Reminder: there’s an event on your calendar.`,\n          items: items.map((event) => {\n            return Object.assign({}, event, {\n              formattedEvent: Formatter.formatEvent(event, tz, now.format(Formatter.formats.YMD), { details: true })\n            });\n          })\n        });\n      }\n    }\n  });\n}",
    "responseTemplate": "{if successResult.hasItems}\n⏰ {successResult.heading}\n\n{for event in successResult.items}\n{event.formattedEvent}\n\n{endfor}\n{else}\nThere’s nothing on your calendar in the next few minutes.\n{endif}\n",
    "inputIds": [],
    "triggers": [{
      "text": "what's on my calendar now",
      "requiresMention": true,
      "isRegex": false,
      "caseSensitive": false,
      "triggerType": TriggerType.MessageSent
    }],
    "config": {
      "exportId": "SBH4IfDzTGO8P7kV02yECw",
      "name": "Reminders",
      "responseTypeId": normalResponseType,
      "isDataType": false,
      "isTest": false
    },
    "exportId": "SBH4IfDzTGO8P7kV02yECw",
  }, {
    "id": "Sb6jPeE0RaSXGbLTBh-GSg",
    "teamId": "v-i65oxZQDiBsZuXceONmA",
    "behaviorId": "NHWvoxtVRU6k5FXmtHnNfw",
    "isNew": false,
    "name": "Setup",
    "description": "",
    "functionBody": "const gcal = require('google-calendar');\nconst cal = new gcal.GoogleCalendar(ellipsis.accessTokens.googleCalendar);\nconst EllipsisApi = ellipsis.require('ellipsis-api');\nconst api = new EllipsisApi(ellipsis).actions;\nconst moment = require('moment-timezone');\nmoment.tz.setDefault(ellipsis.userInfo.timeZone || ellipsis.teamInfo.timeZone);\nlet successMessage = \"\";\nlet calendarName;\n\ncal.calendars.get(\"primary\", (err, res) => {\n  if (err) {\n    throw new ellipsis.Error(`Error retrieving your primary calendar (${err.code}): ${err.message}`, {\n      userMessage: \"Sorry, an error occurred retrieving your primary calendar.\"\n    });\n  } else {\n    calendarName = res.summary;\n    doScheduling();\n  }\n});\n\nfunction doScheduling() {\n  api.unschedule({\n    actionName: \"Agenda\",\n    channel: ellipsis.userInfo.messageInfo.channel,\n    userId: ellipsis.userInfo.ellipsisUserId\n  }).then(r => {\n    return api.unschedule({\n      actionName: \"Reminders\",\n      channel: ellipsis.userInfo.messageInfo.channel,\n      userId: ellipsis.userInfo.ellipsisUserId\n    });\n  }).then(r => {\n    if (whenToAnnounce !== \"none\") {\n      return api.schedule({\n        actionName: \"Agenda\",\n        channel: ellipsis.userInfo.messageInfo.channel,\n        recurrence: `every weekday at ${whenToAnnounce}`\n      });\n    }\n  }).then(r => {\n    const recurrenceText = r.scheduled ? r.scheduled.recurrence : `every weekday at ${whenToAnnounce}`;\n    const nextRecurrence = r.scheduled ? r.scheduled.firstRecurrence : null;\n    const calendarNameText = calendarName ? `the calendar **${calendarName}**` : \"your primary calendar\";\n    successMessage += whenToAnnounce === \"none\" ?\n      `OK. I won’t send you an agenda in this channel.` :\n      `OK! I’ll show you the events on ${calendarNameText} ${recurrenceText} in this channel${\n        nextRecurrence ? `, starting ${moment(nextRecurrence).format(\"dddd, MMMM D\")}` : \"\"\n      }.`;\n    if (shouldRemind) {\n      successMessage += whenToAnnounce === \"none\" ?\n        `\\n\\nHowever, I will send you reminders a few minutes before each event begins.` :\n        `\\n\\nI’ll also send you reminders a few minutes before each event begins.`;\n      return api.schedule({\n        actionName: \"Reminders\",\n        channel: ellipsis.userInfo.messageInfo.channel,\n        recurrence: \"every 5 minutes\"\n      });\n    } else {\n      return true;\n    }\n  }).then(r => {\n    ellipsis.success(successMessage + \"\\n\\nTo change these settings, say “setup my calendar” again.\" )\n  });\n}",
    "responseTemplate": "{successResult}",
    "inputIds": ["fLL6qZ9rSR-gdVXrbz8sEQ", "P1GZEO9nTKKFl5dP707j1A"],
    "triggers": [{
      "text": "set up my calendar",
      "requiresMention": true,
      "isRegex": false,
      "caseSensitive": false,
      "triggerType": TriggerType.MessageSent
    }, {"text": "setup my calendar", "requiresMention": true, "isRegex": false, "caseSensitive": false, "triggerType": TriggerType.MessageSent}],
    "config": {
      "exportId": "ioHMv3b3T4utFwwH0cwjLg",
      "name": "Setup",
      "responseTypeId": normalResponseType,
      "isDataType": false,
      "isTest": false
    },
    "exportId": "ioHMv3b3T4utFwwH0cwjLg"
  }],
  "libraryVersions": [{
    "id": "Q-h3QwrnSjiAU7YAmMPZYg",
    "libraryId": "mmf-B3NiT-KJ2wAjoqpd0w",
    "exportId": "AjtTiS-fTzKYaHsNUUpZqg",
    "isNew": false,
    "name": "eventlib",
    "description": "",
    "functionBody": "return {\n  filterDeclined: function(events) {\n    return events.filter((event) => {\n      const selfAttend = (event.attendees || []).find((ea) => ea.self);\n      const response = selfAttend ? selfAttend.responseStatus : null;\n      return response !== \"declined\";\n    });\n  }\n}\n",
    "createdAt": "2018-04-17T10:19:13.74-04:00"
  }],
  "requiredAWSConfigs": [],
  "requiredOAuthApiConfigs": [{
    "exportId": "aCJZZ3vgS8eU9BAqhxjz6w-RdG2Wm5DR0m2_4FZXf-yKA-googleCalendar",
    "apiId": "RdG2Wm5DR0m2_4FZXf-yKA",
    "recommendedScope": "https://www.googleapis.com/auth/calendar",
    "nameInCode": "googleCalendar"
  }],
  "requiredSimpleTokenApis": [],
  "gitSHA": "af7815b9b42cf277c67f4f7123c8901826ea869e",
  "exportId": "9l9tTPMcQliQRua_UmJ8sw",
  "createdAt": "2018-04-17T10:19:13.74-04:00",
  "isManaged": false
});
const installedMyCalendar: BehaviorGroup = BehaviorGroup.fromJson({
  "id": "sxokL6idQ_a6Ks3QOD-Sug",
  "teamId": "v-i65oxZQDiBsZuXceONmA",
  "name": "My Calendar",
  "description": "Helps you keep track of your personal calendar by sending you a daily agenda and sending reminders before each event throughout the day. Say “setup my calendar” in a direct message to set it up. Or type “my calendar help” for more info.",
  "icon": "⏰",
  "actionInputs": [{
    "id": "eyTIxbGRT8WCE9fE6X_5WQ",
    "inputId": "515vXugFTA6-wZnXl8CzYA",
    "exportId": "w4C6zIa4SNO1X_91WiQTTQ",
    "name": "shouldRemind",
    "paramType": {"id": "Yes/No", "exportId": "Yes/No", "name": "Yes/No", "needsConfig": false, typescriptType: "boolean"},
    "question": "Would you like me to send you a reminder before each event begins?",
    "isSavedForTeam": false,
    "isSavedForUser": false
  }, {
    "id": "1vPU3dh5QtWBdqvkIVbd7w",
    "inputId": "IiSaZaA1Rbe_4JvsIZsBfQ",
    "exportId": "_lpHYaeKQfqd4Gghk3fhGg",
    "name": "whenToAnnounce",
    "paramType": {"id": "Text", "exportId": "Text", "name": "Text", "needsConfig": false, typescriptType: "string"},
    "question": "What time should I send you your agenda for the day? e.g. “9 AM” or “10 AM Eastern time”, or “none” if you don’t want an agenda",
    "isSavedForTeam": false,
    "isSavedForUser": false
  }],
  "dataTypeInputs": [],
  "behaviorVersions": [{
    "id": "_QtJEAlgT_yaoOUl6iPnwA",
    "teamId": "v-i65oxZQDiBsZuXceONmA",
    "behaviorId": "nrRQD4xlRk62cWcP99W4WA",
    "groupId": "sxokL6idQ_a6Ks3QOD-Sug",
    "isNew": false,
    "name": "Deactivate",
    "description": "",
    "functionBody": "const EllipsisApi = ellipsis.require('ellipsis-api');\nconst api = new EllipsisApi(ellipsis).actions;\napi.unschedule({\n  actionName: \"Agenda\",\n  channel: ellipsis.userInfo.messageInfo.channel,\n  userId: ellipsis.userInfo.ellipsisUserId\n}).then(() => {\n  return api.unschedule({\n    actionName: \"Reminders\",\n    channel: ellipsis.userInfo.messageInfo.channel,\n    userId: ellipsis.userInfo.ellipsisUserId\n  });\n}).then(() => {\n  ellipsis.success();\n}).catch((err) => {\n  throw new ellipsis.Error(err, { userMessage: \"I tried to turn off your agenda and calendar reminders, but something went wrong. Try again, or else try unscheduling it manually.\" });\n});",
    "responseTemplate": "OK. I will no longer send you your agenda or reminders in this channel.\n",
    "inputIds": [],
    "triggers": [{
      "text": "deactivate my calendar",
      "requiresMention": true,
      "isRegex": false,
      "caseSensitive": false,
      "triggerType": TriggerType.MessageSent
    }, {
      "text": "stop my calendar",
      "requiresMention": true,
      "isRegex": false,
      "caseSensitive": false,
      "triggerType": TriggerType.MessageSent
    }, {"text": "turn off my calendar", "requiresMention": true, "isRegex": false, "caseSensitive": false, "triggerType": TriggerType.MessageSent}],
    "config": {
      "exportId": "V-LAjv1AS4CoFimMANnxpg",
      "name": "Deactivate",
      "responseTypeId": normalResponseType,
      "isDataType": false,
      "isTest": false
    },
    "exportId": "V-LAjv1AS4CoFimMANnxpg",
    "createdAt": "2018-04-16T15:18:28.21-04:00"
  }, {
    "id": "pURDxka2SK6pMMtgp6P0kA",
    "teamId": "v-i65oxZQDiBsZuXceONmA",
    "behaviorId": "VlBGDA64Qr-RmWGmnfyQPQ",
    "groupId": "sxokL6idQ_a6Ks3QOD-Sug",
    "isNew": false,
    "name": "Help",
    "description": "",
    "functionBody": "",
    "responseTemplate": "Ellipsis can show you what's happening on your calendar today, and send reminders when events are about to begin. (This skill requires access to your Google Calendar.)\n\n**Actions:**\n- `what's on my calendar today` — show your agenda (list all events) for the rest of the day\n- `what's on my calendar now` — show any events happening now or in the next 10 minutes\n- `setup my calendar` — set up the skill to send you your agenda each weekday, and send you reminders a few minutes before events begin\n- `stop my calendar` — turn off the scheduled agenda and reminders\n",
    "inputIds": [],
    "triggers": [{"text": "my calendar help", "requiresMention": true, "isRegex": false, "caseSensitive": false, "triggerType": TriggerType.MessageSent}],
    "config": {
      "exportId": "J0uB9LvZTo6_L-spEBrtqg",
      "name": "Help",
      "responseTypeId": normalResponseType,
      "isDataType": false,
      "isTest": false
    },
    "exportId": "J0uB9LvZTo6_L-spEBrtqg",
    "createdAt": "2018-04-16T15:18:28.166-04:00"
  }, {
    "id": "yD5828gvSgifRejls5_G-A",
    "teamId": "v-i65oxZQDiBsZuXceONmA",
    "behaviorId": "F09FdPl3Qz22wi3h1EfZqQ",
    "groupId": "sxokL6idQ_a6Ks3QOD-Sug",
    "isNew": false,
    "name": "Setup",
    "description": "",
    "functionBody": "const gcal = require('google-calendar');\nconst cal = new gcal.GoogleCalendar(ellipsis.accessTokens.googleCalendar);\nconst EllipsisApi = ellipsis.require('ellipsis-api');\nconst api = new EllipsisApi(ellipsis).actions;\nconst moment = require('moment-timezone');\nmoment.tz.setDefault(ellipsis.userInfo.timeZone || ellipsis.teamInfo.timeZone);\nlet successMessage = \"\";\nlet calendarName;\n\ncal.calendars.get(\"primary\", (err, res) => {\n  if (err) {\n    throw new ellipsis.Error(`Error retrieving your primary calendar (${err.code}): ${err.message}`, {\n      userMessage: \"Sorry, an error occurred retrieving your primary calendar.\"\n    });\n  } else {\n    calendarName = res.summary;\n    doScheduling();\n  }\n});\n\nfunction doScheduling() {\n  api.unschedule({\n    actionName: \"Agenda\",\n    channel: ellipsis.userInfo.messageInfo.channel,\n    userId: ellipsis.userInfo.ellipsisUserId\n  }).then(r => {\n    return api.unschedule({\n      actionName: \"Reminders\",\n      channel: ellipsis.userInfo.messageInfo.channel,\n      userId: ellipsis.userInfo.ellipsisUserId\n    });\n  }).then(r => {\n    if (whenToAnnounce !== \"none\") {\n      return api.schedule({\n        actionName: \"Agenda\",\n        channel: ellipsis.userInfo.messageInfo.channel,\n        recurrence: `every weekday at ${whenToAnnounce}`\n      });\n    }\n  }).then(r => {\n    const recurrenceText = r.scheduled ? r.scheduled.recurrence : `every weekday at ${whenToAnnounce}`;\n    const nextRecurrence = r.scheduled ? r.scheduled.firstRecurrence : null;\n    const calendarNameText = calendarName ? `the calendar **${calendarName}**` : \"your primary calendar\";\n    successMessage += whenToAnnounce === \"none\" ?\n      `OK. I won’t send you an agenda in this channel.` :\n      `OK! I’ll show you the events on ${calendarNameText} ${recurrenceText} in this channel${\n        nextRecurrence ? `, starting ${moment(nextRecurrence).format(\"dddd, MMMM D\")}` : \"\"\n      }.`;\n    if (shouldRemind) {\n      successMessage += whenToAnnounce === \"none\" ?\n        `\\n\\nHowever, I will send you reminders a few minutes before each event begins.` :\n        `\\n\\nI’ll also send you reminders a few minutes before each event begins.`;\n      return api.schedule({\n        actionName: \"Reminders\",\n        channel: ellipsis.userInfo.messageInfo.channel,\n        recurrence: \"every 5 minutes\"\n      });\n    } else {\n      return true;\n    }\n  }).then(r => {\n    ellipsis.success(successMessage + \"\\n\\nTo change these settings, say “setup my calendar” again.\" )\n  });\n}",
    "responseTemplate": "{successResult}",
    "inputIds": ["IiSaZaA1Rbe_4JvsIZsBfQ", "515vXugFTA6-wZnXl8CzYA"],
    "triggers": [{
      "text": "set up my calendar",
      "requiresMention": true,
      "isRegex": false,
      "caseSensitive": false,
      "triggerType": TriggerType.MessageSent
    }, {"text": "setup my calendar", "requiresMention": true, "isRegex": false, "caseSensitive": false, "triggerType": TriggerType.MessageSent}],
    "config": {
      "exportId": "ioHMv3b3T4utFwwH0cwjLg",
      "name": "Setup",
      "responseTypeId": normalResponseType,
      "isDataType": false,
      "isTest": false
    },
    "exportId": "ioHMv3b3T4utFwwH0cwjLg",
    "createdAt": "2018-04-16T15:18:28.021-04:00"
  }, {
    "id": "dLhVuW9XRh2BUM9SmIEYww",
    "teamId": "v-i65oxZQDiBsZuXceONmA",
    "behaviorId": "c0vyvrDrSTiQ8zM9pITxBQ",
    "groupId": "sxokL6idQ_a6Ks3QOD-Sug",
    "isNew": false,
    "name": "Reminders",
    "description": "",
    "functionBody": "const moment = require('moment-timezone');\nmoment.tz.setDefault(ellipsis.userInfo.timeZone || ellipsis.teamInfo.timeZone);\nconst gcal = require('google-calendar');\nconst cal = new gcal.GoogleCalendar(ellipsis.accessTokens.googleCalendar);\nconst Formatter = ellipsis.require('ellipsis-cal-date-format@0.0.13');\nconst eventlib = require('eventlib');\n\nlist();\n\nfunction list() {\n  const now = moment();\n  const min = now.clone();\n  const max = now.clone().add(8, 'minutes');\n  cal.events.list(\"primary\", {\n    timeMin: min.toISOString(),\n    timeMax: max.toISOString(),\n    orderBy: 'startTime',\n    singleEvents: true\n  }, (err, res) => {\n    const errorMessage = \"An error occurred while checking your Google calendar for upcoming events. You may try running `...what's on my calendar now` to see if it happens again. The problem may be temporary.\";\n    if (err) {\n      throw new ellipsis.Error(`Error ${err.code}: ${err.message}`, {\n        userMessage: errorMessage\n      });\n    } else if (!res.items) {\n      throw new ellipsis.Error(\"Google Calendar returned an invalid response (no items).\", {\n        userMessage: errorMessage\n      });\n    } else {\n      const tz = res.timeZone || ellipsis.teamInfo.timeZone;\n      moment.tz.setDefault(tz);\n      const items = eventlib.filterDeclined(res.items.filter((ea) => {\n        return moment(ea.start.dateTime).isAfter(now.clone().add(2, 'minutes').add(30, 'seconds'))\n      }));\n      if (items.length === 0) {\n        if (ellipsis.event.originalEventType === \"scheduled\") {\n          ellipsis.noResponse();\n        } else {\n          ellipsis.success({\n            hasItems: false\n          });\n        }\n      } else {\n        ellipsis.success({\n          hasItems: true,\n          heading: items.length > 1 ?\n            `Reminder: there are ${items.length} events on your calendar.` :\n            `Reminder: there’s an event on your calendar.`,\n          items: items.map((event) => {\n            return Object.assign({}, event, {\n              formattedEvent: Formatter.formatEvent(event, tz, now.format(Formatter.formats.YMD), { details: true })\n            });\n          })\n        });\n      }\n    }\n  });\n}",
    "responseTemplate": "{if successResult.hasItems}\n⏰ {successResult.heading}\n\n{for event in successResult.items}\n{event.formattedEvent}\n\n{endfor}\n{else}\nThere’s nothing on your calendar in the next few minutes.\n{endif}\n",
    "inputIds": [],
    "triggers": [{
      "text": "what's on my calendar now",
      "requiresMention": true,
      "isRegex": false,
      "caseSensitive": false,
      "triggerType": TriggerType.MessageSent
    }],
    "config": {
      "exportId": "SBH4IfDzTGO8P7kV02yECw",
      "name": "Reminders",
      "responseTypeId": normalResponseType,
      "isDataType": false,
      "isTest": false
    },
    "exportId": "SBH4IfDzTGO8P7kV02yECw",
    "createdAt": "2018-04-16T15:18:28.189-04:00"
  }, {
    "id": "vgvsAlqBSXSoeR42UYR5Kg",
    "teamId": "v-i65oxZQDiBsZuXceONmA",
    "behaviorId": "FR5HZbi1Sreh6BFUY8N0Ig",
    "groupId": "sxokL6idQ_a6Ks3QOD-Sug",
    "isNew": false,
    "name": "Agenda",
    "description": "",
    "functionBody": "const moment = require('moment-timezone');\nconst gcal = require('google-calendar');\nconst cal = new gcal.GoogleCalendar(ellipsis.accessTokens.googleCalendar);\nconst Formatter = ellipsis.require('ellipsis-cal-date-format@0.0.13');\nconst eventlib = require('eventlib');\n\ncal.calendars.get(\"primary\", (err, res) => {\n  if (err) {\n    throw new ellipsis.Error(`An error occurred retrieving your primary calendar (${err.code}): ${err.message}`, {\n      userMessage: \"An error occurred while fetching your calendar from Google. You may try running `...what's on my calendar today` again to see if it was temporary.\"\n    });\n  } else {\n    const tz = res.timeZone;\n    list(tz || ellipsis.userInfo.timeZone || ellipsis.teamInfo.timeZone);\n  }\n});\n\nfunction list(tz) {\n  moment.tz.setDefault(tz);\n  const now = moment();\n  const min = now.clone();\n  const max = now.clone().startOf('day').add(1, 'days');\n  cal.events.list(\"primary\", {\n    timeMin: min.toISOString(),\n    timeMax: max.toISOString(),\n    orderBy: 'startTime',\n    singleEvents: true\n  }, (err, res) => {\n    if (err) {\n      ellipsis.error(`An error occurred fetching your calendar. (${err.code}: ${err.message})`);\n    } else if (!res.items) {\n      ellipsis.error(\"There was a problem fetching your calendar. Google Calendar may be experiencing a hiccup.\");\n    } else {\n      const items = eventlib.filterDeclined(res.items.slice());\n      let heading = \"\";\n      if (items.length === 0) {\n        heading = \"🎉 There’s nothing on your calendar for the rest of the day.\";\n      } else if (items.length === 1) {\n        heading = \"There’s 1 event on your calendar today:\";\n      } else {\n        heading = `There are ${items.length} events on your calendar today:`;\n      }\n      const result = {\n        heading: heading,\n        items: items.map((event) => {\n          return Object.assign({}, event, {\n            formattedEvent: Formatter.formatEvent(event, tz, now.format(Formatter.formats.YMD))\n          });\n        })\n      };\n      ellipsis.success(result);\n    }\n  });\n}",
    "responseTemplate": "**{successResult.heading}**\n{for event in successResult.items}\n{event.formattedEvent}\n{endfor}\n",
    "inputIds": [],
    "triggers": [{
      "text": "what's on my calendar today",
      "requiresMention": true,
      "isRegex": false,
      "caseSensitive": false,
      "triggerType": TriggerType.MessageSent
    }],
    "config": {
      "exportId": "BXUYJotxSaKz3QqZ_zSd-w",
      "name": "Agenda",
      "responseTypeId": normalResponseType,
      "isDataType": false,
      "isTest": false
    },
    "exportId": "BXUYJotxSaKz3QqZ_zSd-w",
    "createdAt": "2018-04-16T15:18:28.14-04:00"
  }],
  "libraryVersions": [{
    "id": "1LM70vXTRzqyiJUTh_6K7Q",
    "libraryId": "vZtbwqYwTlazL6lu7Pzefw",
    "exportId": "AjtTiS-fTzKYaHsNUUpZqg",
    "isNew": false,
    "name": "eventlib",
    "description": "",
    "functionBody": "return {\n  filterDeclined: function(events) {\n    return events.filter((event) => {\n      const selfAttend = (event.attendees || []).find((ea) => ea.self);\n      const response = selfAttend ? selfAttend.responseStatus : null;\n      return response !== \"declined\";\n    });\n  }\n}\n",
    "createdAt": "2018-04-16T15:18:27.809-04:00"
  }],
  "requiredAWSConfigs": [],
  "requiredOAuthApiConfigs": [{
    "id": "y7d7pXsyScGEYN_lzrB2ow",
    "exportId": "aCJZZ3vgS8eU9BAqhxjz6w-RdG2Wm5DR0m2_4FZXf-yKA-googleCalendar",
    "apiId": "RdG2Wm5DR0m2_4FZXf-yKA",
    "recommendedScope": "https://www.googleapis.com/auth/calendar",
    "nameInCode": "googleCalendar",
    "config": {
      "apiId": "RdG2Wm5DR0m2_4FZXf-yKA",
      "id": "5vHvtAggQLyJ7E_oXmWeag",
      "scope": "https://www.googleapis.com/auth/calendar",
      "displayName": "Google Calendar"
    }
  }],
  "requiredSimpleTokenApis": [],
  "exportId": "9l9tTPMcQliQRua_UmJ8sw",
  "createdAt": "2018-04-16T15:18:27.673-04:00",
  "author": {
    "ellipsisUserId": "NeBjtWPSTBKnjMMt8zgx-g",
    "userName": "Luke Andrews",
    "fullName": "Luke Andrews",
    "timeZone": "America/New_York"
  },
  "metaData": {
    "groupId": "sxokL6idQ_a6Ks3QOD-Sug",
    "initialCreatedAt": "2018-04-11T15:32:34.921-04:00",
    "initialAuthor": {
      "ellipsisUserId": "NeBjtWPSTBKnjMMt8zgx-g",
      "userName": "Luke Andrews",
      "fullName": "Luke Andrews",
      "timeZone": "America/New_York"
    }
  },
  "isManaged": false
});

function textDiff(left: string, right: string) {
  return MultiLineTextPropertyDiff.maybeFor("", left, right);
}

describe('diffs', () => {

  describe('maybeDiffFor', () => {

    it('builds the correct diff for a behavior group', () => {
      const version1 = BehaviorGroup.fromJson(behaviorGroupVersion1);
      const version2 = BehaviorGroup.fromJson(behaviorGroupVersion2);
      const maybeDiff = maybeDiffFor(version1, version2, null, false);
      expect(maybeDiff).toBeTruthy();
      const diffText = maybeDiff && maybeDiff.displayText();

      // the empty objects for original and modified objects are ignored in the match
      const expectedDiffTree = {
        "children": [
          {
            "isCode": false,
            "label": "Skill name",
            "modified": "Some updated skill",
            "original": "Some skill",
            "unifiedLines": [[
              {
                "kind": "unchanged",
                "value": "Some "
              },
              {
                "kind": "added",
                "value": "updated "
              },
              {
                "kind": "unchanged",
                "value": "skill"
              }
            ]]
          },
          {
            "isCode": false,
            "label": "Skill description",
            "modified": "With a description",
            "original": "",
            "unifiedLines": [[
              {
                "kind": "added",
                "value": "With a description"
              },
            ]],
          },
          {
            "isCode": false,
            "label": "Skill icon",
            "modified": "",
            "original": "🚀",
            "unifiedLines": [[
              {
                "kind": "removed",
                 "value": "🚀"
               }
            ]]
          },
          {
            "children": [
              {
                "isCode": false,
                "label": "Name",
                "modified": "Second name",
                "original": "First name",
                "unifiedLines": [[
                  {
                    "kind": "removed",
                    "value": "First"
                  },
                  {
                    "kind": "added",
                    "value": "Second"
                  },
                  {
                    "kind": "unchanged",
                    "value": " name"
                  }
                ]]
              },
              {
                "isCode": false,
                "label": "Description",
                "modified": "A description",
                "original": "",
                "unifiedLines": [[
                  {
                    "kind": "added",
                    "value": "A description"
                  }
                ]]
              },
              {
                "isCode": true,
                "label": "Response template",
                "modified": "Another template",
                "original": "A template",
                "unifiedLines": [[
                  {
                    "kind": "unchanged",
                    "value": "A"
                  },
                  {
                    "kind": "added",
                    "value": "nother"
                  },
                  {
                    "kind": "unchanged",
                    "value": " template"
                  }
                ]]
              },
              {
                "isCode": true,
                "label": "Code",
                "modified": "use strict; // so strict",
                "original": "use strict;",
                "unifiedLines": [[
                  {
                    "kind": "unchanged",
                    "value": "use strict;"
                  },
                  {
                    "kind": "added",
                    "value": " // so strict"
                  }
                ]]
              },
              {
                "label": "Response type",
                "modified": "Private",
                "original": "Normal",
              },
              {
                "item": {
                  "caseSensitive": false,
                  "isRegex": true,
                  "requiresMention": false,
                  "text": ".+"
                }
              },
              {
                "item": {
                  "caseSensitive": false,
                  "isRegex": false,
                  "requiresMention": false,
                  "text": "C"
                }
              },
              {
                "children": [
                  {
                    "label": "Require user to mention Ellipsis",
                    "modified": true,
                    "original": false
                  }
                ],
                "modified": {
                  "caseSensitive": false,
                  "isRegex": true,
                  "requiresMention": true,
                  "text": "B"
                },
                "original": {
                  "caseSensitive": false,
                  "isRegex": true,
                  "requiresMention": false,
                  "text": "B"
                }
              },
              {
                "children": [
                  {
                    "isCode": false,
                    "label": "Question",
                    "modified": "who drives the car?",
                    "original": "what drives the car?",
                    "unifiedLines": [[
                      {
                        "kind": "unchanged",
                        "value": "wh"
                      },
                      {
                        "kind": "removed",
                        "value": "at"
                      },
                      {
                        "kind": "added",
                        "value": "o"
                      },
                      {
                        "kind": "unchanged",
                        "value": " drives the car?"
                      }
                    ]]
                  },
                  {
                    "label": "Data type",
                    "modified": "Person",
                    "original": "Text"
                  },
                  {
                    "label": "Save and re-use answer for the team",
                    "modified": true,
                    "original": false
                  },
                  {
                    "label": "Save and re-use answer for each user",
                    "modified": false,
                    "original": true
                  }
                ]
              },
              {
                "afterItems": [
                  {
                    name: 'somethingElse',
                    question: 'and now for something?',
                    paramType: {
                      id: 'Text',
                      name: 'Text',
                      exportId: 'Text',
                      needsConfig: false
                    },
                    isSavedForTeam: false,
                    isSavedForUser: false,
                    inputId: inputId2,
                    exportId: inputId2
                  },
                  {
                    name: 'clown',
                    question: 'who drives the car?',
                    paramType: {
                      id: 'sdflkjafks',
                      name: 'Person',
                      exportId: 'sdflkjafks',
                      needsConfig: false
                    },
                    isSavedForTeam: true,
                    isSavedForUser: false,
                    inputId: inputId,
                    exportId: inputId
                  }
                ],
                "beforeItems": [
                  {
                    name: 'clown',
                    question: 'what drives the car?',
                    paramType: {
                      id: 'Text',
                      name: 'Text',
                      exportId: 'Text',
                      needsConfig: false
                    },
                    isSavedForTeam: false,
                    isSavedForUser: true,
                    inputId: inputId,
                    exportId: inputId
                  },
                  {
                    name: 'somethingElse',
                    question: 'and now for something?',
                    paramType: {
                      id: 'Text',
                      name: 'Text',
                      exportId: 'Text',
                      needsConfig: false
                    },
                    isSavedForTeam: false,
                    isSavedForUser: false,
                    inputId: inputId2,
                    exportId: inputId2
                  }
                ]
              }
            ]
          },
          {
            "children": [
              {
                "isCode": false,
                "label": "Name",
                "modified": "some-lib-revised",
                "original": "some-lib",
                "unifiedLines": [[
                  {
                    "kind": "unchanged",
                    "value": "some-lib"
                  },
                  {
                    "kind": "added",
                    "value": "-revised"
                  }
                ]]
              },
              {
                "isCode": false,
                "label": "Description",
                "modified": "A library (revised)",
                "original": "A library",
                "unifiedLines": [[
                  {
                    "kind": "unchanged",
                    "value": "A library"
                  },
                  {
                    "kind": "added",
                    "value": " (revised)"
                  }
                ]]
              },
              {
                "isCode": true,
                "label": "Code",
                "modified": "return \"foo\";",
                "original": "return \"foo\"",
                "unifiedLines": [[
                  {
                    "kind": "unchanged",
                    "value": "return \"foo\""
                  },
                  {
                    "kind": "added",
                    "value": ";"
                  }
                ]]
              }
            ]
          },

          {
            "children": [
              {
                "label": "Configuration to use",
                "modified": "AWS Prod",
                "original": ""
              }
            ]
          },

          {
            "children": [
              {
                "label": "Name used in code",
                "modified": "githubReadonly",
                "original": "github"
              },
              {
                "label": "Recommended scope",
                "modified": "repo:readonly",
                "original": "repo"
              }
            ]
          },

          {
            "children": [
              {
                "label": "Name used in code",
                "modified": "pivotalTracker2",
                "original": "pivotalTracker"
              }
            ]
          }
        ]
      };

      expect(maybeDiff).toMatchObject(expectedDiffTree);

      expect(diffText).toContain("Modified action “First name”");
      expect(diffText).toContain("Name: [-First][+Second] name");
      expect(diffText).toContain("Description: [+A description]");
      expect(diffText).toContain("Response template: A[+nother] template");
      expect(diffText).toContain("Code: use strict;[+ // so strict]");
      expect(diffText).toContain("Removed trigger “C”");
      expect(diffText).toContain("Added trigger “.+”");
      expect(diffText).toContain("Modified trigger “B”:\nRequire user to mention Ellipsis: changed to on");
      expect(diffText).toContain("Modified library “some-lib”");
      expect(diffText).toContain("Name: some-lib[+-revised]");
      expect(diffText).toContain("Description: A library[+ (revised)]");
      expect(diffText).toContain("Code: return \"foo\"[+;]");

    });

    it('recognizes API integration configuration differences by default', () => {
      const maybeDiff = maybeDiffFor(publishedMyCalendar, installedMyCalendar, null, false);
      expect(maybeDiff).not.toBeNull();
      const diff = maybeDiff as ModifiedDiff<BehaviorGroup>;
      expect(diff.children.length).toBe(1);
      const childDiff = diff.children[0] as ModifiedDiff<RequiredOAuthApplication>;
      expect(childDiff).toBeInstanceOf(ModifiedDiff);
      expect(childDiff.original).toBeInstanceOf(RequiredOAuthApplication);
      expect(childDiff.original.config).toBeNull();
      expect(childDiff.modified.config).not.toBeNull();
    });

    it('ignores API integration configuration differences when comparing to published skills', () => {
      const maybeDiff = maybeDiffFor(publishedMyCalendar, installedMyCalendar, null, true);
      expect(maybeDiff).toBeNull();
    });

  });

  describe('MultiLineTextPropertyDiff', () => {
    it('handles a single line', () => {
      const left = `cat`;
      const right = `dog`;
      const result = textDiff(left, right) as MultiLineTextPropertyDiff;
      expect(result.oldLines).toEqual([[new TextPart("cat", false, true)]]);
      expect(result.newLines).toEqual([[new TextPart("dog", true)]]);
      expect(result.unifiedLines).toEqual([[new TextPart("cat", false, true), new TextPart("dog", true)]]);
    });

    it('handles removing a line', () => {
      const left = `cat
dog
bear
`;
      const right = `cat
bear
`;
      const result = textDiff(left, right) as MultiLineTextPropertyDiff;
      expect(result.oldLines).toEqual([
        [new TextPart("cat\n")],
        [new TextPart("dog\n", false, true)],
        [new TextPart("bear\n")],
        []
      ]);
      expect(result.newLines).toEqual([
        [new TextPart("cat\n")],
        [],
        [new TextPart("bear\n")],
        []
      ]);
      expect(result.unifiedLines).toEqual([
        [new TextPart("cat\n")],
        [new TextPart("dog\n", false, true)],
        [new TextPart("bear\n")],
        []
      ]);
    });

    it('handles removing new lines', () => {
      const left = `cat


dog`;
      const right = `cat
dog`;

      const result = textDiff(left, right) as MultiLineTextPropertyDiff;
      expect(result.oldLines).toEqual([
        [new TextPart("cat\n")],
        [new TextPart("\n", false, true)],
        [new TextPart("\n", false, true)],
        [new TextPart("dog")]
      ]);
      expect(result.newLines).toEqual([
        [new TextPart("cat\n")],
        [],
        [],
        [new TextPart("dog")]
      ]);
      expect(result.unifiedLines).toEqual([
        [new TextPart("cat\n")],
        [new TextPart("\n", false, true)],
        [new TextPart("\n", false, true)],
        [new TextPart("dog")]
      ]);
    });

    it('handles adding new lines', () => {
      const left = `cat
dog`;
      const right = `cat

dog

`;
      const result = textDiff(left, right) as MultiLineTextPropertyDiff;
      expect(result.oldLines).toEqual([
        [new TextPart("cat\n")],
        [],
        [{ kind: "unchanged", value: "dog"}]
      ]);
      expect(result.newLines).toEqual([
        [new TextPart("cat\n")],
        [new TextPart("\n", true)],
        [{ kind: "unchanged", value: "dog"}, new TextPart("\n", true)],
        [new TextPart("\n", true)],
        []
      ]);
      expect(result.unifiedLines).toEqual([
        [new TextPart("cat\n")],
        [new TextPart("\n", true)],
        [{ kind: "unchanged", value: "dog"}, new TextPart("\n", true)],
        [new TextPart("\n", true)],
        []
      ]);
    });

    it('mixed changes', () => {
      const left = `in an old house in paris
all covered with vines
lived twelve little girls
in two straight lines`;
      const right = `in a new house in nice, all covered with bricks

lived twelve little boys

with two straight sticks`;
      const result = textDiff(left, right) as MultiLineTextPropertyDiff;
      expect(result.oldLines).toEqual([
        [new TextPart("in a"), new TextPart("n old", false, true),
          new TextPart(" house in "), new TextPart("paris\n", false, true)],
        [new TextPart("all covered with "), new TextPart("vines", false, true), new TextPart("\n")],
        [new TextPart("lived twelve little "), new TextPart("girls\n", false, true)],
        [],
        [new TextPart("in", false, true), new TextPart(" two straight "), new TextPart("line", false, true), new TextPart("s")]
      ]);
      expect(result.newLines).toEqual([
        [new TextPart("in a"),
          { kind: "added", value: " new"},
          new TextPart(" house in "), { kind: "added", value: "nice, "},
          { kind: "unchanged", value: "all covered with "}, new TextPart("bricks\n", true)],
        [new TextPart("\n")],
        [{ kind: "unchanged", value: "lived twelve little "}, { kind: "added", value: "boys\n"}],
        [new TextPart("\n", true)],
        [new TextPart("with", true), { kind: "unchanged", value: " two straight "}, new TextPart("stick", true), new TextPart("s")]
      ]);
    });
  });

});
