import React from 'react';
import TestUtils from 'react-addons-test-utils';
const BehaviorListApp = require('../app/assets/javascripts/behavior_list/app');
const BehaviorList = require('../app/assets/javascripts/behavior_list/index');
const TimeZoneSetter = require('../app/assets/javascripts/time_zone_setter/index');
const BehaviorGroup = require('../app/assets/javascripts/models/behavior_group');
jest.mock('../app/assets/javascripts/lib/data_request', () => ({
  jsonGet: jest.fn(() => {
    return new Promise((resolve, reject) => {
      process.nextTick(() => resolve([]) || reject({ error: "oops" }));
    });
  }),
  jsonPost: jest.fn(() => {
    return new Promise((resolve, reject) => {
      process.nextTick(() => resolve([]) || reject({ error: "oops" }));
    });
  })
}));

describe('BehaviorListApp', () => {
  jsRoutes.controllers.ApplicationController.fetchPublishedBehaviorInfo = () => '/fetch';
  jsRoutes.controllers.BehaviorEditorController.newGroup = () => '/newGroup';
  jsRoutes.controllers.ApplicationController.possibleCitiesFor = () => '/possibleCitiesFor';

  const behaviorVersionTask1 = Object.freeze({
    "teamId": "abcdef",
    "groupId": "sfgsdf",
    "behaviorId": "ghijkl",
    "functionBody": "use strict;",
    "responseTemplate": "A template",
    "params": [],
    "triggers": [{
      "text": "B",
      "requiresMention": false,
      "isRegex": true,
      "caseSensitive": false
    }, {
      "text": "C",
      "requiresMention": false,
      "isRegex": false,
      "caseSensitive": false
    }],
    "config": {
      "aws": {
        "accessKeyName": "AWS_ACCESS_KEY",
        "secretKeyName": "AWS_SECRET_KEY",
        "regionName": "AWS_REGION"
      }
    },
    "createdAt": 1468338136532
  });
  const behaviorVersionTask2 = Object.freeze({
    "teamId": "abcdef",
    "groupId": "gsdfgsg",
    "behaviorId": "mnopqr",
    "functionBody": "use strict;",
    "responseTemplate": "A template",
    "params": [],
    "triggers": [{
      "text": "A",
      "requiresMention": true,
      "isRegex": true,
      "caseSensitive": false
    }],
    "config": {
      "aws": {
        "accessKeyName": "AWS_ACCESS_KEY",
        "secretKeyName": "AWS_SECRET_KEY",
        "regionName": "AWS_REGION"
      }
    },
    "createdAt": 1468359271138
  });
  const behaviorVersionKnowledge1 = Object.freeze({
    "teamId": "abcdef",
    "groupId": "jfghjfg",
    "behaviorId": "stuvwx",
    "functionBody": "",
    "responseTemplate": "The magic 8-ball says:\n\n“Concentrate and ask again.”",
    "params": [],
    "triggers": [],
    "config": {},
    "createdAt": 1466109904858
  });
  const group1 = Object.freeze({
    id: "a",
    name: "A",
    description: "",
    behaviorVersions: [behaviorVersionTask1],
    libraryVersions: [],
    createdAt: 1466109904858
  });
  const group2 = Object.freeze({
    id: "b",
    name: "B",
    description: "",
    behaviorVersions: [behaviorVersionTask2],
    libraryVersions: [],
    createdAt: 1466109904858
  });
  const group3 = Object.freeze({
    id: "c",
    name: "",
    description: "",
    behaviorVersions: [behaviorVersionKnowledge1],
    libraryVersions: [],
    createdAt: 1466109904858
  });
  const defaultConfig = Object.freeze({
    csrfToken: "1",
    behaviorGroups: [group1, group2, group3],
    teamId: "1",
    slackTeamId: "1",
    teamTimeZone: "America/Toronto",
    branchName: null
  });

  function createBehaviorListApp(config) {
    return TestUtils.renderIntoDocument(
      <BehaviorListApp {...config} />
    );
  }

  let config = {};

  beforeEach(() => {
    config = Object.assign({}, defaultConfig);
  });

  describe('render', () => {
    it('renders a BehaviorList when the time zone is set', () => {
      const list = createBehaviorListApp(config);
      expect(TestUtils.scryRenderedComponentsWithType(list, BehaviorList).length).toBe(1);
      expect(TestUtils.scryRenderedComponentsWithType(list, TimeZoneSetter).length).toBe(0);
    });

    it('renders a TimeZoneSetter when no time zone is set', () => {
      config.teamTimeZone = null;
      const list = createBehaviorListApp(config);
      expect(TestUtils.scryRenderedComponentsWithType(list, BehaviorList).length).toBe(0);
      expect(TestUtils.scryRenderedComponentsWithType(list, TimeZoneSetter).length).toBe(1);
    });
  });

  describe('didUpdateExistingGroup', () => {
    const installedGroup = Object.assign({}, group1, { id: "1", exportId: "2", name: "old" });
    const installedGroup2 = Object.assign({}, group2, { id: "2", exportId: "2" });
    const materializedGroup = BehaviorGroup.fromJson(installedGroup);
    const updatedGroup = Object.assign({}, installedGroup, { name: "new" });

    it('removes the existing group from currently installing and adds it to recently installed', () => {
      config.localBehaviorGroups = [installedGroup];
      const list = createBehaviorListApp(config);
      list.setState = jest.fn();
      list.state = {
        recentlyInstalled: [installedGroup2],
        currentlyInstalling: [materializedGroup]
      };
      list.didUpdateExistingGroup(materializedGroup, updatedGroup);
      expect(list.setState).toHaveBeenCalledWith({
        recentlyInstalled: [installedGroup2, updatedGroup],
        currentlyInstalling: []
      });
    });

    it('splices the updated group in if it was previously recently installed', () => {
      config.localBehaviorGroups = [installedGroup];
      const list = createBehaviorListApp(config);
      list.setState = jest.fn();
      list.state = {
        recentlyInstalled: [installedGroup, installedGroup2],
        currentlyInstalling: [materializedGroup]
      };
      list.didUpdateExistingGroup(materializedGroup, updatedGroup);
      expect(list.setState).toHaveBeenCalledWith({
        recentlyInstalled: [updatedGroup, installedGroup2],
        currentlyInstalling: []
      });
    });
  });
});
