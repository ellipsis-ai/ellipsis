import * as React from 'react';
import * as TestUtils from 'react-addons-test-utils';
import BehaviorList from '../../../../app/assets/frontend/behavior_list/index';
import BehaviorGroup from '../../../../app/assets/frontend/models/behavior_group';
import BehaviorGroupCard from '../../../../app/assets/frontend/behavior_list/behavior_group_card';
import Page from '../../../../app/assets/frontend/shared_ui/page';

describe('BehaviorList', () => {
  jsRoutes.controllers.BehaviorEditorController.edit = () => ({ url: '/edit', method: 'get' });
  jsRoutes.controllers.BehaviorEditorController.newGroup = () => ({ url: '/newGroup', method: 'get' });
  jsRoutes.controllers.ApplicationController.possibleCitiesFor = () => ({ url: '/possibleCitiesFor', method: 'get' });

  const behaviorVersionTask1 = Object.freeze({
    "teamId": "abcdef",
    "groupId": "sfgsdf",
    "behaviorId": "b1",
    "name": "THE task",
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
    "behaviorId": "b2",
    "name": "Some task",
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
  const behaviorVersionTask3 = Object.freeze({
    "teamId": "abcdef",
    "groupId": "gsdfgsg",
    "behaviorId": "b3",
    "name": "A task",
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
    "createdAt": 1511817369237
  });
  const behaviorVersionKnowledge1 = Object.freeze({
    "teamId": "abcdef",
    "groupId": "jfghjfg",
    "behaviorId": "b4",
    "name": "Knowledge 1",
    "functionBody": "",
    "responseTemplate": "The magic 8-ball says:\n\n“Concentrate and ask again.”",
    "params": [],
    "triggers": [],
    "config": {},
    "createdAt": 1466109904858
  });
  const behaviorVersionKnowledge2 = Object.freeze({
    "teamId": "abcdef",
    "groupId": "klmnop",
    "behaviorId": "b5",
    "name": "Knowledge 2",
    "functionBody": "",
    "responseTemplate": "The magic 8-ball says:\n\n“Concentrate and ask again.”",
    "params": [],
    "triggers": [],
    "config": {},
    "createdAt": 1511816895686
  });
  const group1 = Object.freeze(BehaviorGroup.fromJson({
    id: "a",
    name: "A",
    description: "",
    behaviorVersions: [behaviorVersionTask1, behaviorVersionTask3],
    libraryVersions: [],
    requiredAWSConfigs: [],
    requiredOAuth2ApiConfigs: [],
    requiredSimpleTokenApis: [],
    createdAt: 1466109904858
  }));
  const group2 = Object.freeze(BehaviorGroup.fromJson({
    id: "b",
    name: "B",
    description: "",
    behaviorVersions: [behaviorVersionTask2],
    libraryVersions: [],
    requiredAWSConfigs: [],
    requiredOAuth2ApiConfigs: [],
    requiredSimpleTokenApis: [],
    createdAt: 1466109904858
  }));
  const group3 = Object.freeze(BehaviorGroup.fromJson({
    id: "c",
    name: "",
    description: "",
    behaviorVersions: [behaviorVersionKnowledge2, behaviorVersionKnowledge1],
    libraryVersions: [],
    requiredAWSConfigs: [],
    requiredOAuth2ApiConfigs: [],
    requiredSimpleTokenApis: [],
    createdAt: 1466109904858
  }));
  const defaultConfig = Object.freeze({
    onLoadPublishedBehaviorGroups: jest.fn(),
    onBehaviorGroupImport: jest.fn(),
    onBehaviorGroupUpdate: jest.fn(),
    onMergeBehaviorGroups: jest.fn(),
    onDeleteBehaviorGroups: jest.fn(),
    onBehaviorGroupDeploy: jest.fn(),
    onSearch: jest.fn(),
    localBehaviorGroups: [group1, group2, group3],
    publishedBehaviorGroups: [],
    recentlyInstalled: [],
    currentlyInstalling: [],
    matchingResults: {},
    isDeploying: false,
    deployError: null,
    publishedBehaviorGroupLoadStatus: 'loaded',
    teamId: "1",
    slackTeamId: "1",
    botName: "TestBot",
    notification: null
  });

  class Footer extends React.Component {
    renderFooter(content) {
      return (
        <div>{content}</div>
      );
    }

    render() {
      return (
        <div />
      );
    }
  }

  function createBehaviorList(config) {
    const footer = TestUtils.renderIntoDocument(<Footer/>);
    return TestUtils.renderIntoDocument(
      <BehaviorList {...config} {...Page.requiredPropDefaults()} onRenderFooter={footer.renderFooter} />
    );
  }

  let config = {};

  beforeEach(() => {
    config = Object.assign({}, defaultConfig);
  });

  describe('render', () => {
    it('renders a card for each group', () => {
      const list = createBehaviorList(config);
      expect(TestUtils.scryRenderedComponentsWithType(list, BehaviorGroupCard).length).toEqual(config.localBehaviorGroups.length);
    });
  });

  describe('getLocalBehaviorGroups', () => {
    it('returns the intersection of original groups, updated and newly installed', () => {
      const original1 = group1.clone({ id: "a", exportId: "1", name: "original" });
      const original2 = group2.clone({ id: "b", exportId: null });
      config.localBehaviorGroups = [original1, original2];

      const updated1 = group1.clone({ id: "a", exportId: "1", name: "updated!" });
      const brandNew = group3.clone({ name: "new!", exportId: "2" });
      config.recentlyInstalled = [updated1, brandNew];

      const list = createBehaviorList(config);
      expect(list.getLocalBehaviorGroups()).toEqual([updated1, original2, brandNew]);
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
