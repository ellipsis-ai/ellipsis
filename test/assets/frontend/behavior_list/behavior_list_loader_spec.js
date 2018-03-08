import * as React from 'react';
import * as TestUtils from 'react-addons-test-utils';
import BehaviorListLoader from '../../../../app/assets/frontend/behavior_list/loader';
import BehaviorList from '../../../../app/assets/frontend/behavior_list/index';
import TimeZoneSetter from '../../../../app/assets/frontend/time_zone/team_time_zone_setter';
import BehaviorGroup from '../../../../app/assets/frontend/models/behavior_group';

jest.setMock('../../../../app/assets/frontend/lib/data_request', { DataRequest: () => ({
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
})});

describe('BehaviorListApp', () => {
  jsRoutes.controllers.ApplicationController.fetchPublishedBehaviorInfo = () => ({ url: '/fetch', method: 'get' });
  jsRoutes.controllers.ApplicationController.possibleCitiesFor = () => ({ url: '/possibleCitiesFor', method: 'get' });
  jsRoutes.controllers.BehaviorEditorController.edit = () => ({ url: '/edit', method: 'get' });
  jsRoutes.controllers.BehaviorEditorController.newGroup = () => ({ url: '/newGroup', method: 'get' });

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
    "config": {},
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
    "config": {},
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
  const group1 = {
    id: "a",
    teamId: "1",
    name: "A",
    icon: null,
    description: "",
    githubUrl: null,
    actionInputs: [],
    dataTypeInputs: [],
    behaviorVersions: [behaviorVersionTask1],
    libraryVersions: [],
    requiredAWSConfigs: [],
    requiredOAuth2ApiConfigs: [],
    requiredSimpleTokenApis: [],
    createdAt: 1466109904858,
    exportId: null,
    author: null,
    gitSHA: null,
    deployment: null
  };
  const group2 = {
    id: "b",
    teamId: "1",
    name: "B",
    icon: null,
    description: "",
    githubUrl: null,
    actionInputs: [],
    dataTypeInputs: [],
    behaviorVersions: [behaviorVersionTask2],
    libraryVersions: [],
    requiredAWSConfigs: [],
    requiredOAuth2ApiConfigs: [],
    requiredSimpleTokenApis: [],
    createdAt: 1466109904858,
    exportId: null,
    author: null,
    gitSHA: null,
    deployment: null
  };
  const group3 = {
    id: "c",
    teamId: "1",
    name: "",
    icon: null,
    description: "",
    githubUrl: null,
    actionInputs: [],
    dataTypeInputs: [],
    behaviorVersions: [behaviorVersionKnowledge1],
    libraryVersions: [],
    requiredAWSConfigs: [],
    requiredOAuth2ApiConfigs: [],
    requiredSimpleTokenApis: [],
    createdAt: 1466109904858,
    exportId: null,
    author: null,
    gitSHA: null,
    deployment: null
  };
  const defaultConfig = {
    containerId: "foo",
    csrfToken: "1",
    behaviorGroups: [group1, group2, group3],
    teamId: "1",
    slackTeamId: "1",
    teamTimeZone: "America/Toronto",
    branchName: null,
    botName: "TestBot"
  };

  function createBehaviorListLoader(config) {
    const div = document.createElement("div");
    return TestUtils.renderIntoDocument(
      <BehaviorListLoader {...config} feedbackContainer={div} />
    );
  }

  let config = {};

  beforeEach(() => {
    config = Object.assign({}, defaultConfig);
  });

  describe('render', () => {
    it('renders a BehaviorList when the time zone is set', () => {
      const list = createBehaviorListLoader(config);
      expect(TestUtils.scryRenderedComponentsWithType(list, BehaviorList).length).toBe(1);
      expect(TestUtils.scryRenderedComponentsWithType(list, TimeZoneSetter).length).toBe(0);
    });

    it('renders a TimeZoneSetter when no time zone is set', () => {
      config.teamTimeZone = null;
      const list = createBehaviorListLoader(config);
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
      const list = createBehaviorListLoader(config);
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
      const list = createBehaviorListLoader(config);
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
