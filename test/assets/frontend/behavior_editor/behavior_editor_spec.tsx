jest.mock('../../../../app/assets/frontend/behavior_editor/code_editor');
jest.mock('../../../../app/assets/frontend/shared_ui/react-codemirror', () => function() {
  return null;
});
import * as MockDataRequest from '../../../mocks/mock_data_request';
jest.mock('../../../../app/assets/frontend/lib/data_request', () => MockDataRequest);

import * as React from 'react';
import * as TestUtils from 'react-addons-test-utils';
import BehaviorEditor from '../../../../app/assets/frontend/behavior_editor/index';
import BehaviorVersion, {BehaviorVersionJson} from '../../../../app/assets/frontend/models/behavior_version';
import BehaviorGroup, {BehaviorGroupJson} from '../../../../app/assets/frontend/models/behavior_group';
import ParamType from '../../../../app/assets/frontend/models/param_type';
import {AWSConfigRef} from '../../../../app/assets/frontend/models/aws';
import {OAuthApplicationRef} from '../../../../app/assets/frontend/models/oauth';
import {SimpleTokenApiRef} from '../../../../app/assets/frontend/models/simple_token';

jsRoutes.controllers.BehaviorEditorController.edit = jest.fn(() => ({ url: '/mock_edit' }));
jsRoutes.controllers.BehaviorEditorController.save = jest.fn(() => ({ url: '/mock_save' }));
jsRoutes.controllers.BehaviorEditorController.newGroup = jest.fn(() => ({ url: '/mock_new_skill' }));
jsRoutes.controllers.ApplicationController.index = jest.fn(() => ({ url: '/mock_index' }));
jsRoutes.controllers.ApplicationController.deleteBehaviorGroups = jest.fn(() => ({ url: '/mock_delete_behavior_group' }));
jsRoutes.controllers.BehaviorEditorController.edit = jest.fn(() => ({ url: '/mock_edit' }));
jsRoutes.controllers.BehaviorEditorController.nodeModuleVersionsFor = jest.fn(() => ({ url: '/mock_node_module_versions_for' }));
jsRoutes.controllers.BehaviorEditorController.testResults = jest.fn(() => ({ url: '/mock_behavior_test_results' }));
jsRoutes.controllers.SocialAuthController.authenticateGithub = jest.fn(() => ({ url: '/mock_authenticate_github' }));
jsRoutes.controllers.BehaviorEditorController.versionInfoFor = jest.fn(() => ({ url: '/mock_version_info' }));

describe('BehaviorEditor', () => {
  const groupJson: BehaviorGroupJson = {
    id: '1',
    actionInputs: [],
    behaviorVersions: [
      {
        id: "123abcdef",
        behaviorId: "1",
        functionBody: "onSuccess('Woot')",
        responseTemplate: "{successResult}",
        inputIds: [],
        triggers: [{
          text: "Do the tests run?",
          requiresMention: false,
          isRegex: false,
          caseSensitive: false
        }],
        config: {
          isDataType: false,
          isTest: false
        },
        knownEnvVarsUsed: [],
        groupId: '1'
      }
    ],
    libraryVersions: [],
    requiredAWSConfigs: [],
    requiredOAuthApiConfigs: [],
    requiredSimpleTokenApis: [],
    dataTypeInputs: [],
    isManaged: false,
    linkedGithubRepo: {
      owner: "ellipsis-ai",
      repo: "ellipsis",
      currentBranch: "master"
    }
  };
  const defaultConfig = Object.freeze({
    teamId: "A",
    "isAdmin": false,
    "isLinkedToGithub": false,
    group: groupJson,
    selectedId: "1",
    csrfToken: "2",
    envVariables: [ { name: "HOT_DOG" } ],
    builtinParamTypes: [{
      id: 'Text',
      name: 'Text',
      needsConfig: false,
      exportId: 'Text'
    }, {
      id: 'Number',
      name: 'Number',
      needsConfig: false,
      exportId: 'Number'
    }],
    "awsConfigs": [{
      "id": "aws",
      "displayName": "main",
      "accessKeyId": "a",
      "secretAccessKey": "b",
      "region": "c"
    }],
    "oauthApplications": [{
      "apiId": "7gK5ysNxSjSa9BzfB44yAg",
      "applicationId": "R1-v9CKHTEmaUvgei-GmIg",
      "scope": "read",
      "displayName": "Trello"
    }, {
      "apiId": "RdG2Wm5DR0m2_4FZXf-yKA",
      "applicationId": "Yy1QcMTcT96tZZmUoYLroQ",
      "scope": "https://www.googleapis.com/auth/calendar",
      "displayName": "Google Calendar"
    }],
    "oauthApis": [{
      "apiId": "7gK5ysNxSjSa9BzfB44yAg",
      "name": "Trello",
      "newApplicationUrl": "https://trello.com/app-key",
      "scopeDocumentationUrl": "",
      "isOAuth1": true
    }, {
      "apiId": "RdG2Wm5DR0m2_4FZXf-yKA",
      "name": "Google",
      "requiresAuth": true,
      "newApplicationUrl": "https://console.developers.google.com/apis",
      "scopeDocumentationUrl": "https://developers.google.com/identity/protocols/googlescopes",
      "isOAuth1": false
    }],
    "simpleTokenApis": [{
      "apiId": "pivotal-tracker",
      "displayName": "Pivotal Tracker",
      "tokenUrl": "https://www.pivotaltracker.com/profile",
      "logoImageUrl": "/assets/images/logos/pivotal_tracker.png"
    }],
    "linkedOAuthApplicationIds": ["R1-v9CKHTEmaUvgei-GmIg", "Yy1QcMTcT96tZZmUoYLroQ"],
    onSave: jest.fn(),
    savedAnswers: [],
    onForgetSavedAnswerForInput: jest.fn(),
    userId: "1",
    onLinkGithubRepo: jest.fn(),
    onUpdateFromGithub: jest.fn()
  });

  const newGroupJson: BehaviorGroupJson = {
    "teamId": "B",
    "actionInputs": [],
    "dataTypeInputs": [],
    "behaviorVersions": [{
      "id": "1",
      "teamId": "B",
      "behaviorId": "2",
      "isNew": true,
      "functionBody": "",
      "responseTemplate": "",
      "inputIds": [],
      "triggers": [{ "text": "", "requiresMention": true, "isRegex": false, "caseSensitive": false }],
      "config": { "isDataType": false, "isTest": false },
      "knownEnvVarsUsed": []
    }],
    "libraryVersions": [],
    "requiredAWSConfigs": [],
    "requiredOAuthApiConfigs": [],
    "requiredSimpleTokenApis": [],
    "createdAt": "2017-09-15T11:58:07.36-04:00",
    "author": { "id": "3", userName: "attaboy" },
    isManaged: false
  };

  const newSkillConfig = Object.freeze({
    "containerId": "editorContainer",
    "csrfToken": "1234",
    "isAdmin": false,
    "isLinkedToGithub": false,
    "group": newGroupJson,
    "builtinParamTypes": [{ "id": "Text", "exportId": "Text", "name": "Text", "needsConfig": false }, {
      "id": "Number",
      "exportId": "Number",
      "name": "Number",
      "needsConfig": false
    }, { "id": "Yes/No", "exportId": "Yes/No", "name": "Yes/No", "needsConfig": false }],
    "envVariables": [{
      "name": "OH_REALLY",
      "isAlreadySavedWithValue": false
    }],
    "savedAnswers": [],
    "awsConfigs": [{
      "id": "aws",
      "displayName": "main",
      "accessKeyId": "a",
      "secretAccessKey": "b",
      "region": "c"
    }],
    "oauthApplications": [{
      "apiId": "7gK5ysNxSjSa9BzfB44yAg",
      "applicationId": "R1-v9CKHTEmaUvgei-GmIg",
      "scope": "read",
      "displayName": "Trello"
    }, {
      "apiId": "RdG2Wm5DR0m2_4FZXf-yKA",
      "applicationId": "Yy1QcMTcT96tZZmUoYLroQ",
      "scope": "https://www.googleapis.com/auth/calendar",
      "displayName": "Google Calendar"
    }],
    "oauthApis": [{
      "apiId": "7gK5ysNxSjSa9BzfB44yAg",
      "name": "Trello",
      "newApplicationUrl": "https://trello.com/app-key",
      "scopeDocumentationUrl": "",
      "isOAuth1": true
    }, {
      "apiId": "RdG2Wm5DR0m2_4FZXf-yKA",
      "name": "Google",
      "requiresAuth": true,
      "newApplicationUrl": "https://console.developers.google.com/apis",
      "scopeDocumentationUrl": "https://developers.google.com/identity/protocols/googlescopes",
      "isOAuth1": false
    }],
    "simpleTokenApis": [{
      "apiId": "pivotal-tracker",
      "displayName": "Pivotal Tracker",
      "tokenUrl": "https://www.pivotaltracker.com/profile",
      "logoImageUrl": "/assets/images/logos/pivotal_tracker.png"
    }],
    "linkedOAuthApplicationIds": ["R1-v9CKHTEmaUvgei-GmIg", "Yy1QcMTcT96tZZmUoYLroQ"],
    "userId": "3",
    selectedId: "2",
    onSave: jest.fn(),
    onForgetSavedAnswerForInput: jest.fn(),
    onLinkGithubRepo: jest.fn(),
    onUpdateFromGithub: jest.fn()
  });

  let editorConfig;
  let firstBehavior: BehaviorVersionJson;

  beforeEach(function() {
    editorConfig = Object.assign({}, defaultConfig);
    firstBehavior = editorConfig.group.behaviorVersions[0];
  });

  function createEditor(config): BehaviorEditor {
    const props = Object.assign({}, config, {
      group: BehaviorGroup.fromJson(config.group),
      awsConfigs: config.awsConfigs.map(AWSConfigRef.fromJson),
      oauthApplications: config.oauthApplications.map(OAuthApplicationRef.fromJson),
      simpleTokenApis: config.simpleTokenApis.map(SimpleTokenApiRef.fromJson),
      builtinParamTypes: config.builtinParamTypes.map(ParamType.fromJson),
      onDeploy: jest.fn(),
      botName: "TestBot"
    });
    return TestUtils.renderIntoDocument(
      <BehaviorEditor {...props} />
    ) as BehaviorEditor;
  }

  describe('getFunctionBody', () => {
    it('returns the defined function', () => {
      firstBehavior.functionBody = 'return;';
      let editor = createEditor(editorConfig);
      expect(editor.getFunctionBody()).toEqual('return;');
    });

    it('returns a string even when no function is defined', () => {
      delete firstBehavior.functionBody;
      let editor = createEditor(editorConfig);
      expect(editor.getFunctionBody()).toEqual("");
    });
  });

  describe('getInputs', () => {
    it('returns the defined parameters', () => {
      editorConfig.group.actionInputs = [{
        name: 'clown',
        question: 'what drives the car?',
        paramType: editorConfig.builtinParamTypes[0],
        isSavedForTeam: false,
        isSavedForUser: true,
        inputId: "abcd1234",
        exportId: null
      }];
      firstBehavior.inputIds = editorConfig.group.actionInputs.map(ea => ea.inputId);
      let editor = createEditor(editorConfig);
      expect(editor.getInputs()).toMatchObject(editorConfig.group.actionInputs);
    });

    it('returns an array even when no params are defined', () => {
      delete firstBehavior.inputIds;
      let editor = createEditor(editorConfig);
      expect(editor.getInputs()).toEqual([]);
    });
  });

  describe('getBehaviorTemplate', () => {
    it('returns the template when it’s non-empty', () => {
      firstBehavior.responseTemplate = 'clowncar';
      let editor = createEditor(editorConfig);
      expect(editor.getBehaviorTemplate().toString()).toEqual('clowncar');
    });

    it('returns a default template when no template is defined', () => {
      delete firstBehavior.responseTemplate;
      let editor = createEditor(editorConfig);
      expect(editor.getBehaviorTemplate().toString()).toEqual('');
   });

    it('returns a default template when the template is blank', () => {
      firstBehavior.responseTemplate = '';
      let editor = createEditor(editorConfig);
      expect(editor.getBehaviorTemplate().toString()).toEqual('');
    });

    it('returns the template when it’s empty on an existing skill', () => {
      firstBehavior.responseTemplate = '';
      let editor = createEditor(editorConfig);
      expect(editor.getBehaviorTemplate().toString()).toEqual('');
    });

    it('returns the default template on a new, empty skill', () => {
      let editor = createEditor(newSkillConfig);
      expect(editor.getBehaviorTemplate().toString()).toEqual(BehaviorVersion.defaultActionProps().responseTemplate.toString());
    });
  });

  describe('render', () => {
    it("renders the normal editor when isDataType is false", () => {
      firstBehavior.config.isDataType = false;
      const editor = createEditor(editorConfig);
      const dataSpy = jest.spyOn(editor, 'renderDataTypeBehavior');
      const normalSpy = jest.spyOn(editor, 'renderNormalBehavior');
      editor.render();
      expect(dataSpy).not.toBeCalled();
      expect(normalSpy).toBeCalled();
    });
    it("renders the data type editor when isDataType is true", () => {
      const bv: BehaviorVersionJson = editorConfig.group.behaviorVersions[0];
      bv.config.isDataType = true;
      bv.config.dataTypeConfig = { fields: [], usesCode: true };
      const editor = createEditor(editorConfig);
      const dataSpy = jest.spyOn(editor, 'renderDataTypeBehavior');
      const normalSpy = jest.spyOn(editor, 'renderNormalBehavior');
      editor.render();
      expect(dataSpy).toBeCalled();
      expect(normalSpy).not.toBeCalled();
    });
    it("renders for a new, unsaved skill with an action", () => {
      const config = Object.assign({}, newSkillConfig);
      const editor = createEditor(config);
      const normalSpy = jest.spyOn(editor, 'renderNormalBehavior');
      editor.render();
      expect(normalSpy).toBeCalled();
    });
  });

  describe('isJustSaved', () => {
    const HALF_MINUTE = 30000;
    const TWO_MINUTES = 120000;
    it("false if recent save but isModified() is true", () => {
      let config = Object.assign({}, editorConfig, {
        group: Object.assign({}, editorConfig.group, { createdAt: Number(new Date()) - HALF_MINUTE })
      });
      let editor = createEditor(config);
      editor.isModified = jest.fn(() => true);
      let justSaved = editor.isJustSaved();
      expect(justSaved).toEqual(false);
    });

    it("false if not a recent save", () => {
      let config = Object.assign({}, editorConfig, {
        group: Object.assign({}, editorConfig.group, { createdAt: Number(new Date()) - TWO_MINUTES })
      });
      let editor = createEditor(config);
      editor.isModified = jest.fn(() => false);
      let justSaved = editor.isJustSaved();
      expect(justSaved).toEqual(false);
    });

    it("true if a recent save and isModified() is false", () => {
      let config = Object.assign({}, editorConfig, {
        group: Object.assign({}, editorConfig.group, { createdAt: Number(new Date()) - HALF_MINUTE })
      });
      let editor = createEditor(config);
      editor.isModified = jest.fn(() => false);
      let justSaved = editor.isJustSaved();
      expect(justSaved).toEqual(true);
    });
  });

  describe('getOtherSavedInputsInGroup', () => {
    it("returns the (unique by inputId) saved params", () => {
      const groupId = editorConfig.group.id;
      const inputId = "abcd12345";
      const savedAnswerInput = {
        name: 'foo',
        question: '',
        paramType: editorConfig.builtinParamTypes[0],
        isSavedForTeam: false,
        isSavedForUser: true,
        inputId: inputId,
        groupId: groupId
      };
      const otherBehaviorsInGroup: Array<BehaviorVersionJson> = [
          {
            teamId: "1",
            behaviorId: "2",
            functionBody: "",
            responseTemplate: "",
            inputIds: [savedAnswerInput.inputId],
            triggers: [],
            config: {
              isDataType: false,
              isTest: false
            },
            knownEnvVarsUsed: [],
            groupId: groupId
          },
          {
            teamId: "1",
            behaviorId: "3",
            functionBody: "",
            responseTemplate: "",
            inputIds: [savedAnswerInput.inputId],
            triggers: [],
            config: {
              isDataType: false,
              isTest: false
            },
            knownEnvVarsUsed: [],
            groupId: groupId
          }
        ].map(ea => BehaviorVersion.fromJson(ea));
      let allVersions = editorConfig.group.behaviorVersions.concat(otherBehaviorsInGroup);
      let newGroup = Object.assign({}, editorConfig.group, { actionInputs: [savedAnswerInput], behaviorVersions: allVersions });
      let config = Object.assign({}, editorConfig, { group: newGroup });
      let editor = createEditor(config);
      expect(editor.getOtherSavedInputsInGroup().map(ea => ea.inputId)).toEqual([otherBehaviorsInGroup[0].inputIds[0]]);
    });
  });

  describe('setConfigProperty', () => {
    it("clones the existing behavior config with updated properties", () => {
      editorConfig.group.behaviorVersions[0].config.forcePrivateResponse = false;
      let editor = createEditor(editorConfig);
      editor.setEditableProp = jest.fn();
      expect(editor.getBehaviorConfig().forcePrivateResponse).toBe(false);
      editor.setConfigProperty('forcePrivateResponse', true);
      const newConfig = editor.setEditableProp.mock.calls[0][1];
      expect(newConfig.constructor.name).toBe("BehaviorConfig");
      expect(newConfig.forcePrivateResponse).toBe(true);
    });
  });

  describe('componentDidMount', () => {
    it("does not show the version browser if the initial props do not have showVersions: true", () => {
      const editor = createEditor(editorConfig);
      editor.showVersions = jest.fn();
      editor.componentDidMount();
      expect(editor.showVersions).not.toHaveBeenCalled();
    });
    it("shows the version browser if the initial props have showVersions: true", () => {
      editorConfig.showVersions = true;
      const editor = createEditor(editorConfig);
      editor.showVersions = jest.fn();
      editor.componentDidMount();
      expect(editor.showVersions).toHaveBeenCalled();
    });
  });
});
