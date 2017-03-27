import React from 'react';
import TestUtils from 'react-addons-test-utils';
const BehaviorList = require('../app/assets/javascripts/behavior_list/index');
const BehaviorGroup = require('../app/assets/javascripts/models/behavior_group');
const BehaviorGroupCard = require('../app/assets/javascripts/behavior_list/behavior_group_card');

describe('BehaviorList', () => {
  jsRoutes.controllers.BehaviorEditorController.edit = () => '/edit';
  jsRoutes.controllers.BehaviorEditorController.newGroup = () => '/newGroup';

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
  const group1 = Object.freeze(BehaviorGroup.fromJson({id:"sfgsdf", name:"", description: "", behaviorVersions: [behaviorVersionTask1], createdAt: 1466109904858}));
  const group2 = Object.freeze(BehaviorGroup.fromJson({id:"gsdfgsg", name:"", description: "", behaviorVersions: [behaviorVersionTask2], createdAt: 1466109904858}));
  const group3 = Object.freeze(BehaviorGroup.fromJson({id:"jfghjfg", name:"", description: "", behaviorVersions: [behaviorVersionKnowledge1], createdAt: 1466109904858}));
  const defaultConfig = Object.freeze({
    onLoadPublishedBehaviorGroups: jest.fn(),
    onBehaviorGroupImport: jest.fn(),
    onMergeBehaviorGroups: jest.fn(),
    onDeleteBehaviorGroups: jest.fn(),
    onSearch: jest.fn(),
    localBehaviorGroups: [group1, group2, group3],
    publishedBehaviorGroups: [],
    recentlyInstalled: [],
    matchingResults: [],
    currentSearchText: "",
    isLoadingMatchingResults: false,
    publishedBehaviorGroupLoadStatus: 'loaded',
    teamId: "1",
    slackTeamId: "1"
  });

  function createBehaviorList(config) {
    return TestUtils.renderIntoDocument(
      <BehaviorList {...config} />
    ).refs.component;
  }

  let config = {};

  beforeEach(() => {
    config = Object.assign(config, defaultConfig);
  });

  describe('render', () => {
    it('renders a card for each group', () => {
      const list = createBehaviorList(config);
      expect(TestUtils.scryRenderedComponentsWithType(list, BehaviorGroupCard).length).toEqual(config.localBehaviorGroups.length);
    });
  });

  describe('toggleInfoPanel', () => {
    let list;

    beforeEach(() => {
      jest.clearAllTimers();
      jest.clearAllMocks();
      jest.useFakeTimers();
      list = createBehaviorList(config);
      list.clearActivePanel = jest.fn();
      list.toggleActivePanel = jest.fn();
    });

    it('closes the active panel if it was already open when no group provided', () => {
      list.getActivePanelName = jest.fn(() => "moreInfo");
      list.toggleInfoPanel();
      expect(list.clearActivePanel).toBeCalled();
      expect(list.toggleActivePanel).not.toBeCalled();
    });

    it('closes the active panel if it was already open when same group provided', () => {
      list.getActivePanelName = jest.fn(() => "moreInfo");
      list.setState({
        selectedBehaviorGroup: group1
      });
      list.toggleInfoPanel(group1);
      expect(list.clearActivePanel).toBeCalled();
      expect(list.toggleActivePanel).not.toBeCalled();
    });

    it('re-opens the panel immediately for the same group when closed', () => {
      list.getActivePanelName = jest.fn(() => "");
      list.setState({
        selectedBehaviorGroup: group1
      });
      list.toggleInfoPanel(group1);
      expect(list.clearActivePanel).not.toBeCalled();
      expect(list.toggleActivePanel).toBeCalledWith('moreInfo');
    });

    it('opens the panel immediately if provided with a new group', () => {
      list.getActivePanelName = jest.fn(() => "");
      list.setState({
        selectedBehaviorGroup: group1
      });
      list.setState = jest.fn((newState, callback) => {
        callback();
      });
      list.toggleInfoPanel(group2);
      expect(setTimeout).not.toBeCalled();
      expect(list.setState.mock.calls[0][0].selectedBehaviorGroup).toBe(group2);
      expect(list.toggleActivePanel).toBeCalledWith('moreInfo');
    });

    it('closes the active panel, waits for animation, then opens the new one when switching groups', () => {
      list.getActivePanelName = jest.fn(() => "moreInfo");
      list.setState({
        selectedBehaviorGroup: group1
      });
      list.setState = jest.fn((newState, callback) => {
        callback();
      });
      list.toggleInfoPanel(group2);
      expect(list.clearActivePanel).toBeCalled();
      expect(setTimeout).toBeCalled();
      jest.runOnlyPendingTimers();
      expect(list.setState.mock.calls[0][0].selectedBehaviorGroup).toBe(group2);
      expect(list.toggleActivePanel).toBeCalledWith('moreInfo');
    });
  });
});
