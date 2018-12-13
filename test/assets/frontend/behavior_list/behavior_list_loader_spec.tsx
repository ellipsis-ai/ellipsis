import * as React from 'react';
import * as TestUtils from 'react-addons-test-utils';
import BehaviorListLoader, {BehaviorListLoaderProps} from '../../../../app/assets/frontend/behavior_list/loader';
import BehaviorList from '../../../../app/assets/frontend/behavior_list/index';
import TimeZoneSetter from '../../../../app/assets/frontend/time_zone/team_time_zone_setter';
import BehaviorGroup, {BehaviorGroupJson} from '../../../../app/assets/frontend/models/behavior_group';
import {BehaviorVersionJson} from "../../../../app/assets/frontend/models/behavior_version";
import {ComponentClass} from "react";
import {TriggerType} from "../../../../app/assets/frontend/models/trigger";

jest.setMock('../../../../app/assets/frontend/lib/data_request', { DataRequest: () => ({
  jsonGet: jest.fn(() => {
    return new Promise((resolve) => {
      global.process.nextTick(() => resolve([]));
    });
  }),
  jsonPost: jest.fn(() => {
    return new Promise((resolve) => {
      global.process.nextTick(() => resolve([]));
    });
  })
})});

const absoluteUrl = () => "https://nope/";

describe('BehaviorListApp', () => {
  jsRoutes.controllers.ApplicationController.fetchPublishedBehaviorInfo = () => ({ url: '/fetch', method: 'get', absoluteURL: absoluteUrl });
  jsRoutes.controllers.ApplicationController.possibleCitiesFor = () => ({ url: '/possibleCitiesFor', method: 'get', absoluteURL: absoluteUrl });
  jsRoutes.controllers.BehaviorEditorController.edit = () => ({ url: '/edit', method: 'get', absoluteURL: absoluteUrl });
  jsRoutes.controllers.BehaviorEditorController.newGroup = () => ({ url: '/newGroup', method: 'get', absoluteURL: absoluteUrl });
  jsRoutes.controllers.GithubConfigController.index = () => ({ url: '/githubConfig', method: 'get', absoluteURL: absoluteUrl });

  const normalResponseType= "Normal";
  const normalResponseTypeJson = { id: normalResponseType, displayString: "Display normally" };

  const behaviorVersionTask1: BehaviorVersionJson = {
    "teamId": "abcdef",
    "groupId": "sfgsdf",
    "behaviorId": "ghijkl",
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
    "config": {
      isDataType: false,
      isTest: false,
      responseTypeId: normalResponseType
    },
    "createdAt": 1468338136532,
    inputIds: []
  };
  const behaviorVersionTask2: BehaviorVersionJson = {
    "teamId": "abcdef",
    "groupId": "gsdfgsg",
    "behaviorId": "mnopqr",
    "functionBody": "use strict;",
    "responseTemplate": "A template",
    "triggers": [{
      "text": "A",
      "requiresMention": true,
      "isRegex": true,
      "caseSensitive": false,
      "triggerType": TriggerType.MessageSent
    }],
    "config": {
      isDataType: false,
      isTest: false,
      responseTypeId: normalResponseType
    },
    "createdAt": 1468359271138,
    inputIds: []
  };
  const behaviorVersionKnowledge1: BehaviorVersionJson ={
    "teamId": "abcdef",
    "groupId": "jfghjfg",
    "behaviorId": "stuvwx",
    "functionBody": "",
    "responseTemplate": "The magic 8-ball says:\n\n“Concentrate and ask again.”",
    "triggers": [],
    "config": {
      isDataType: false,
      isTest: false,
      responseTypeId: normalResponseType
    },
    "createdAt": 1466109904858,
    inputIds: []
  };
  const group1: BehaviorGroupJson = {
    id: "a",
    teamId: "1",
    name: "A",
    icon: null,
    description: "",
    actionInputs: [],
    dataTypeInputs: [],
    behaviorVersions: [behaviorVersionTask1],
    libraryVersions: [],
    requiredAWSConfigs: [],
    requiredOAuthApiConfigs: [],
    requiredSimpleTokenApis: [],
    createdAt: 1466109904858,
    exportId: null,
    author: null,
    gitSHA: null,
    deployment: null,
    isManaged: false
  };
  const group2: BehaviorGroupJson = {
    id: "b",
    teamId: "1",
    name: "B",
    icon: null,
    description: "",
    actionInputs: [],
    dataTypeInputs: [],
    behaviorVersions: [behaviorVersionTask2],
    libraryVersions: [],
    requiredAWSConfigs: [],
    requiredOAuthApiConfigs: [],
    requiredSimpleTokenApis: [],
    createdAt: 1466109904858,
    exportId: null,
    author: null,
    gitSHA: null,
    deployment: null,
    isManaged: false
  };
  const group3: BehaviorGroupJson = {
    id: "c",
    teamId: "1",
    name: "",
    icon: null,
    description: "",
    actionInputs: [],
    dataTypeInputs: [],
    behaviorVersions: [behaviorVersionKnowledge1],
    libraryVersions: [],
    requiredAWSConfigs: [],
    requiredOAuthApiConfigs: [],
    requiredSimpleTokenApis: [],
    createdAt: 1466109904858,
    exportId: null,
    author: null,
    gitSHA: null,
    deployment: null,
    isManaged: false
  };
  const defaultConfig: BehaviorListLoaderProps = {
    containerId: "foo",
    csrfToken: "1",
    behaviorGroups: [group1, group2, group3],
    teamId: "1",
    slackTeamId: "1",
    teamTimeZone: "America/Toronto",
    branchName: null,
    botName: "TestBot",
    isLinkedToGithub: false
  };

  function createBehaviorListLoader(config: BehaviorListLoaderProps) {
    const div = document.createElement("div");
    return TestUtils.renderIntoDocument(
      <BehaviorListLoader {...config} feedbackContainer={div} />
    ) as BehaviorListLoader;
  }

  let config: BehaviorListLoaderProps;

  beforeEach(() => {
    config = Object.assign({}, defaultConfig);
  });

  describe('render', () => {
    it('renders a BehaviorList when the time zone is set', () => {
      const list = createBehaviorListLoader(config);
      expect(TestUtils.scryRenderedComponentsWithType(list, BehaviorList as ComponentClass<any>).length).toBe(1);
      expect(TestUtils.scryRenderedComponentsWithType(list, TimeZoneSetter as ComponentClass<any>).length).toBe(0);
    });

    it('renders a TimeZoneSetter when no time zone is set', () => {
      config.teamTimeZone = null;
      const list = createBehaviorListLoader(config);
      expect(TestUtils.scryRenderedComponentsWithType(list, BehaviorList as ComponentClass<any>).length).toBe(0);
      expect(TestUtils.scryRenderedComponentsWithType(list, TimeZoneSetter as ComponentClass<any>).length).toBe(1);
    });
  });

  describe('didUpdateExistingGroup', () => {
    const installedGroup = Object.assign({}, group1, { id: "1", exportId: "2", name: "old" });
    const materializedGroup = BehaviorGroup.fromJson(installedGroup);
    const installedGroup2 = Object.assign({}, group2, { id: "2", exportId: "2" });
    const materializedGroup2 = BehaviorGroup.fromJson(installedGroup2);
    const updatedGroup = BehaviorGroup.fromJson(Object.assign({}, installedGroup, { name: "new" }));

    it('removes the existing group from currently installing and adds it to recently installed', () => {
      config.behaviorGroups = [installedGroup];
      const list = createBehaviorListLoader(config);
      list.setState({
        recentlyInstalled: [materializedGroup2],
        currentlyInstalling: [materializedGroup]
      });
      list.setState = jest.fn();
      list.didUpdateExistingGroup(materializedGroup, updatedGroup);
      expect(list.setState).toHaveBeenCalledWith({
        recentlyInstalled: [materializedGroup2, updatedGroup],
        currentlyInstalling: []
      }, expect.any(Function));
    });

    it('splices the updated group in if it was previously recently installed', () => {
      config.behaviorGroups = [installedGroup];
      const list = createBehaviorListLoader(config);
      list.setState({
        recentlyInstalled: [materializedGroup, materializedGroup2],
        currentlyInstalling: [materializedGroup]
      });
      list.setState = jest.fn();
      list.didUpdateExistingGroup(materializedGroup, updatedGroup);
      expect(list.setState).toHaveBeenCalledWith({
        recentlyInstalled: [updatedGroup, materializedGroup2],
        currentlyInstalling: []
      }, expect.any(Function));
    });
  });
});
