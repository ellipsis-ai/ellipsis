import * as React from 'react';
import * as TestUtils from 'react-dom/test-utils';
import BehaviorList, {BehaviorListProps} from '../../../../app/assets/frontend/behavior_list/index';
import BehaviorGroup from '../../../../app/assets/frontend/models/behavior_group';
import BehaviorGroupCard from '../../../../app/assets/frontend/behavior_list/behavior_group_card';
import {BehaviorVersionJson} from "../../../../app/assets/frontend/models/behavior_version";
import {TriggerType} from "../../../../app/assets/frontend/models/trigger";
import {getPageRequiredProps} from "../../../mocks/mock_page";

const absoluteUrl = () => "https://nope/";

describe('BehaviorList', () => {
  jsRoutes.controllers.BehaviorEditorController.edit = () => ({ url: '/edit', method: 'get', absoluteURL: absoluteUrl });
  jsRoutes.controllers.BehaviorEditorController.newGroup = () => ({ url: '/newGroup', method: 'get', absoluteURL: absoluteUrl });
  jsRoutes.controllers.ApplicationController.possibleCitiesFor = () => ({ url: '/possibleCitiesFor', method: 'get', absoluteURL: absoluteUrl });
  jsRoutes.controllers.GithubConfigController.index = () => ({ url: '/githubConfig', method: 'get', absoluteURL: absoluteUrl });

  const normalResponseType = "Normal";

  const behaviorVersionTask1: BehaviorVersionJson = {
    "teamId": "abcdef",
    "groupId": "sfgsdf",
    "behaviorId": "b1",
    "name": "THE task",
    "functionBody": "use strict;",
    "responseTemplate": "A template",
    "inputIds": [],
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
      "responseTypeId": normalResponseType,
      isDataType: false,
      isTest: false
    },
    "createdAt": 1468338136532
  };
  const behaviorVersionTask2: BehaviorVersionJson = {
    "teamId": "abcdef",
    "groupId": "gsdfgsg",
    "behaviorId": "b2",
    "name": "Some task",
    "functionBody": "use strict;",
    "responseTemplate": "A template",
    "inputIds": [],
    "triggers": [{
      "text": "A",
      "requiresMention": true,
      "isRegex": true,
      "caseSensitive": false,
      "triggerType": TriggerType.MessageSent
    }],
    "config": {
      "responseTypeId": normalResponseType,
      isDataType: false,
      isTest: false
    },
    "createdAt": 1468359271138
  };
  const behaviorVersionTask3: BehaviorVersionJson = {
    "teamId": "abcdef",
    "groupId": "gsdfgsg",
    "behaviorId": "b3",
    "name": "A task",
    "functionBody": "use strict;",
    "responseTemplate": "A template",
    "inputIds": [],
    "triggers": [{
      "text": "A",
      "requiresMention": true,
      "isRegex": true,
      "caseSensitive": false,
      "triggerType": TriggerType.MessageSent
    }],
    "config": {
      "responseTypeId": normalResponseType,
      isDataType: false,
      isTest: false
    },
    "createdAt": 1511817369237
  };
  const behaviorVersionKnowledge1: BehaviorVersionJson = {
    "teamId": "abcdef",
    "groupId": "jfghjfg",
    "behaviorId": "b4",
    "name": "Knowledge 1",
    "functionBody": "",
    "responseTemplate": "The magic 8-ball says:\n\n“Concentrate and ask again.”",
    "inputIds": [],
    "triggers": [],
    "config": {
      "responseTypeId": normalResponseType,
      isDataType: false,
      isTest: false
    },
    "createdAt": 1466109904858
  };
  const behaviorVersionKnowledge2: BehaviorVersionJson = {
    "teamId": "abcdef",
    "groupId": "klmnop",
    "behaviorId": "b5",
    "name": "Knowledge 2",
    "functionBody": "",
    "responseTemplate": "The magic 8-ball says:\n\n“Concentrate and ask again.”",
    "inputIds": [],
    "triggers": [],
    "config": {
      "responseTypeId": normalResponseType,
      isDataType: false,
      isTest: false
    },
    "createdAt": 1511816895686
  }
  const group1 = BehaviorGroup.fromJson({
    teamId: "1",
    id: "a",
    name: "A",
    description: "",
    behaviorVersions: [behaviorVersionTask1, behaviorVersionTask3],
    actionInputs: [],
    dataTypeInputs: [],
    libraryVersions: [],
    requiredAWSConfigs: [],
    requiredOAuthApiConfigs: [],
    requiredSimpleTokenApis: [],
    createdAt: 1466109904858,
    isManaged: false
  });
  const group2 = BehaviorGroup.fromJson({
    teamId: "1",
    id: "b",
    name: "B",
    description: "",
    behaviorVersions: [behaviorVersionTask2],
    actionInputs: [],
    dataTypeInputs: [],
    libraryVersions: [],
    requiredAWSConfigs: [],
    requiredOAuthApiConfigs: [],
    requiredSimpleTokenApis: [],
    createdAt: 1466109904858,
    isManaged: false
  });
  const group3 = BehaviorGroup.fromJson({
    teamId: "1",
    id: "c",
    name: "",
    description: "",
    behaviorVersions: [behaviorVersionKnowledge2, behaviorVersionKnowledge1],
    actionInputs: [],
    dataTypeInputs: [],
    libraryVersions: [],
    requiredAWSConfigs: [],
    requiredOAuthApiConfigs: [],
    requiredSimpleTokenApis: [],
    createdAt: 1466109904858,
    isManaged: false
  });
  const defaultConfig: BehaviorListProps = {
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
    notification: null,
    csrfToken: "hi",
    isLinkedToGithub: false
  };

  class Footer extends React.Component {
    renderFooter(content: React.ReactNode) {
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

  function createBehaviorList(config: BehaviorListProps) {
    const footer = TestUtils.renderIntoDocument(<Footer/>) as Footer;
    return TestUtils.renderIntoDocument(
      <BehaviorList {...config} {...getPageRequiredProps()} onRenderFooter={footer.renderFooter} />
    ) as BehaviorList;
  }

  let config: BehaviorListProps;

  beforeEach(() => {
    config = Object.assign({}, defaultConfig);
  });

  describe('render', () => {
    it('renders a card for each group', () => {
      const list = createBehaviorList(config);
      expect(TestUtils.scryRenderedComponentsWithType(list, BehaviorGroupCard as React.ComponentClass<any>).length).toEqual(config.localBehaviorGroups.length);
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
    let list: BehaviorList;

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
      const setStateMock = jest.fn((newState, callback) => {
        callback();
      });
      list.setState = setStateMock;
      list.toggleInfoPanel(group2);
      expect(setTimeout).not.toBeCalled();
      expect(setStateMock.mock.calls[0][0].selectedBehaviorGroup).toBe(group2);
      expect(list.toggleActivePanel).toBeCalledWith('moreInfo');
    });

    it('closes the active panel, waits for animation, then opens the new one when switching groups', () => {
      list.getActivePanelName = jest.fn(() => "moreInfo");
      list.setState({
        selectedBehaviorGroup: group1
      });
      const setStateMock = jest.fn((newState, callback) => {
        callback();
      });
      list.setState = setStateMock;
      list.toggleInfoPanel(group2);
      expect(list.clearActivePanel).toBeCalled();
      expect(setTimeout).toBeCalled();
      jest.runOnlyPendingTimers();
      expect(setStateMock.mock.calls[0][0].selectedBehaviorGroup).toBe(group2);
      expect(list.toggleActivePanel).toBeCalledWith('moreInfo');
    });
  });
});
