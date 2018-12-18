import * as MockDataRequest from '../../../mocks/mock_data_request';
import * as React from 'react';
import * as TestUtils from 'react-dom/test-utils';
import BehaviorEditor, {BehaviorEditorProps} from '../../../../app/assets/frontend/behavior_editor/index';
import BehaviorVersion from '../../../../app/assets/frontend/models/behavior_version';
import BehaviorGroup from '../../../../app/assets/frontend/models/behavior_group';
import ParamType from '../../../../app/assets/frontend/models/param_type';
import {TriggerType} from "../../../../app/assets/frontend/models/trigger";
import {AWSConfigRef} from '../../../../app/assets/frontend/models/aws';
import {OAuthApplicationRef} from '../../../../app/assets/frontend/models/oauth';
import {SimpleTokenApiRef} from '../../../../app/assets/frontend/models/simple_token';
import {PageRequiredProps} from "../../../../app/assets/frontend/shared_ui/page";
import BehaviorResponseType from "../../../../app/assets/frontend/models/behavior_response_type";
import Input from "../../../../app/assets/frontend/models/input";
import ResponseTemplate from "../../../../app/assets/frontend/models/response_template";
import DataTypeConfig from "../../../../app/assets/frontend/models/data_type_config";
import {getPageRequiredProps} from "../../../mocks/mock_page";

jest.mock('../../../../app/assets/frontend/behavior_editor/code_configuration', () => {
  return {
    default: () => (
      <div/>
    )
  }
});
jest.mock('../../../../app/assets/frontend/behavior_editor/response_template_configuration', () => {
  return {
    default: () => (
      <div/>
    )
  }
});
jest.mock('../../../../app/assets/frontend/lib/data_request', () => MockDataRequest);
jest.mock('emoji-mart/css/emoji-mart.css', () => '');

global.fetch = jest.fn(function() {
  return new Promise(() => {})
});

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
  const normalResponseType = "Normal";
  const normalResponseTypeJson = { id: normalResponseType, displayString: "Respond normally" };
  const privateResponseType = "Private";
  const defaultGroup: BehaviorGroup = BehaviorGroup.fromJson({
    teamId: "1",
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
          caseSensitive: false,
          triggerType: TriggerType.MessageSent
        }],
        config: {
          isDataType: false,
          isTest: false,
          responseTypeId: normalResponseType
        },
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
  });
  const defaultConfig: BehaviorEditorProps = Object.freeze({
    teamId: "A",
    "isAdmin": false,
    "isLinkedToGithub": false,
    group: defaultGroup,
    selectedId: "1",
    csrfToken: "2",
    envVariables: [ { name: "HOT_DOG", isAlreadySavedWithValue: true } ],
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
    }].map(ParamType.fromJson),
    "awsConfigs": [{
      "id": "aws",
      "displayName": "main",
      "accessKeyId": "a",
      "secretAccessKey": "b",
      "region": "c"
    }].map(AWSConfigRef.fromJson),
    "oauthApplications": [{
      "id": "OAuthNumber1",
      "apiId": "7gK5ysNxSjSa9BzfB44yAg",
      "scope": "read",
      "displayName": "Trello",
    }, {
      "id": "OAuthNumber2",
      "apiId": "RdG2Wm5DR0m2_4FZXf-yKA",
      "scope": "https://www.googleapis.com/auth/calendar",
      "displayName": "Google Calendar",
      recommendedScope: ""
    }].map(OAuthApplicationRef.fromJson),
    "oauthApis": [{
      "apiId": "7gK5ysNxSjSa9BzfB44yAg",
      "name": "Trello",
      "newApplicationUrl": "https://trello.com/app-key",
      "scopeDocumentationUrl": "",
      "isOAuth1": true,
      requiresAuth: false
    }, {
      "apiId": "RdG2Wm5DR0m2_4FZXf-yKA",
      "name": "Google",
      "requiresAuth": true,
      "newApplicationUrl": "https://console.developers.google.com/apis",
      "scopeDocumentationUrl": "https://developers.google.com/identity/protocols/googlescopes",
      "isOAuth1": false
    }],
    "simpleTokenApis": [{
      "id": "pivotal-tracker",
      "displayName": "Pivotal Tracker",
      "logoImageUrl": "/assets/images/logos/pivotal_tracker.png"
    }].map(SimpleTokenApiRef.fromJson),
    "linkedOAuthApplicationIds": ["R1-v9CKHTEmaUvgei-GmIg", "Yy1QcMTcT96tZZmUoYLroQ"],
    onSave: jest.fn(),
    savedAnswers: [],
    onForgetSavedAnswerForInput: jest.fn(),
    userId: "1",
    onLinkGithubRepo: jest.fn(),
    onUpdateFromGithub: jest.fn(),
    onLoad: null,
    onDeploy: jest.fn(),
    showVersions: false,
    lastDeployTimestamp: null,
    slackTeamId: "T1",
    botName: "TestBot",
    possibleResponseTypes: [normalResponseTypeJson].map(BehaviorResponseType.fromProps)
  });

  const newGroup: BehaviorGroup = BehaviorGroup.fromJson({
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
      "triggers": [{ "text": "", "requiresMention": true, "isRegex": false, "caseSensitive": false, triggerType: TriggerType.MessageSent }],
      "config": { "isDataType": false, "isTest": false, responseTypeId: normalResponseType },
    }],
    "libraryVersions": [],
    "requiredAWSConfigs": [],
    "requiredOAuthApiConfigs": [],
    "requiredSimpleTokenApis": [],
    "createdAt": "2017-09-15T11:58:07.36-04:00",
    "author": { "ellipsisUserId": "3", userName: "attaboy" },
    isManaged: false
  });

  const newSkillConfig: BehaviorEditorProps = Object.freeze({
    "containerId": "editorContainer",
    "csrfToken": "1234",
    "isAdmin": false,
    "isLinkedToGithub": false,
    "group": newGroup,
    "builtinParamTypes": [{
      "id": "Text",
      "exportId": "Text",
      "name": "Text",
      "needsConfig": false,
      typescriptType: "string"
    }, {
      "id": "Number",
      "exportId": "Number",
      "name": "Number",
      "needsConfig": false,
      typescriptType: "number"
    }, {
      "id": "Yes/No",
      "exportId": "Yes/No",
      "name": "Yes/No",
      "needsConfig": false,
      typescriptType: "boolean"
    }].map(ParamType.fromJson),
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
    }].map(AWSConfigRef.fromJson),
    "oauthApplications": [{
      id: "1",
      "apiId": "7gK5ysNxSjSa9BzfB44yAg",
      "applicationId": "R1-v9CKHTEmaUvgei-GmIg",
      "scope": "read",
      "displayName": "Trello"
    }, {
      id: "2",
      "apiId": "RdG2Wm5DR0m2_4FZXf-yKA",
      "applicationId": "Yy1QcMTcT96tZZmUoYLroQ",
      "scope": "https://www.googleapis.com/auth/calendar",
      "displayName": "Google Calendar"
    }].map(OAuthApplicationRef.fromJson),
    "oauthApis": [{
      "apiId": "7gK5ysNxSjSa9BzfB44yAg",
      "name": "Trello",
      "newApplicationUrl": "https://trello.com/app-key",
      "scopeDocumentationUrl": "",
      "isOAuth1": true,
      requiresAuth: false
    }, {
      "apiId": "RdG2Wm5DR0m2_4FZXf-yKA",
      "name": "Google",
      "requiresAuth": true,
      "newApplicationUrl": "https://console.developers.google.com/apis",
      "scopeDocumentationUrl": "https://developers.google.com/identity/protocols/googlescopes",
      "isOAuth1": false
    }],
    "simpleTokenApis": [{
      id: "1",
      "apiId": "pivotal-tracker",
      "displayName": "Pivotal Tracker",
      "tokenUrl": "https://www.pivotaltracker.com/profile",
      "logoImageUrl": "/assets/images/logos/pivotal_tracker.png",
      requiresAuth: false
    }].map(SimpleTokenApiRef.fromJson),
    "linkedOAuthApplicationIds": ["R1-v9CKHTEmaUvgei-GmIg", "Yy1QcMTcT96tZZmUoYLroQ"],
    "userId": "3",
    selectedId: "2",
    onSave: jest.fn(),
    onForgetSavedAnswerForInput: jest.fn(),
    onLinkGithubRepo: jest.fn(),
    onUpdateFromGithub: jest.fn(),
    onLoad: null,
    onDeploy: jest.fn(),
    showVersions: false,
    lastDeployTimestamp: null,
    slackTeamId: "T1",
    botName: "TestBot",
    possibleResponseTypes: [normalResponseTypeJson].map(BehaviorResponseType.fromProps)
  });

  let editorConfig: BehaviorEditorProps;

  beforeEach(function() {
    editorConfig = Object.assign({}, defaultConfig);
  });

  function createEditor(config: BehaviorEditorProps): BehaviorEditor {
    const props: BehaviorEditorProps & PageRequiredProps = Object.assign({}, getPageRequiredProps(), config);
    return TestUtils.renderIntoDocument(
      <BehaviorEditor {...props} />
    ) as BehaviorEditor;
  }

  describe('getFunctionBody', () => {
    it('returns the defined function', () => {
      editorConfig.group = editorConfig.group.behaviorVersions[0].buildUpdatedGroupFor(editorConfig.group, {
        functionBody: 'return;'
      });
      let editor = createEditor(editorConfig);
      expect(editor.getFunctionBody()).toEqual('return;');
    });

    it('returns a string even when no function is defined', () => {
      editorConfig.group = editorConfig.group.behaviorVersions[0].buildUpdatedGroupFor(editorConfig.group, {
        functionBody: undefined
      });
      let editor = createEditor(editorConfig);
      expect(editor.getFunctionBody()).toEqual("");
    });
  });

  describe('getInputs', () => {
    it('returns inputs in the order of the input IDs', () => {
      editorConfig.group = editorConfig.group.clone({
        actionInputs: [{
          name: "thing1",
          question: "What is thing 1?",
          paramType: editorConfig.builtinParamTypes[0],
          isSavedForTeam: false,
          isSavedForUser: false,
          inputId: "12345"
        }, {
          name: "thing2",
          question: "What is thing 2?",
          paramType: editorConfig.builtinParamTypes[1],
          isSavedForTeam: false,
          isSavedForUser: false,
          inputId: "54321"
        }].map(Input.fromJson),
        behaviorVersions: [editorConfig.group.behaviorVersions[0].clone({
          inputIds: ["54321", "12345"]
        })]
      });
      const editor = createEditor(editorConfig);
      const inputs = editor.getInputs();
      expect(inputs[0].inputId).toBe("54321");
      expect(inputs[1].inputId).toBe("12345");
    });
  });

  describe('getBehaviorTemplate', () => {
    it('returns the template when it’s non-empty', () => {
      editorConfig.group = editorConfig.group.behaviorVersions[0].buildUpdatedGroupFor(editorConfig.group, {
        responseTemplate: ResponseTemplate.fromString('clowncar')
      });
      let editor = createEditor(editorConfig);
      const template = editor.getBehaviorTemplate();
      expect(template && template.toString()).toEqual('clowncar');
    });

    it('returns an empty template when no template is defined', () => {
      editorConfig.group = editorConfig.group.behaviorVersions[0].buildUpdatedGroupFor(editorConfig.group, {
        responseTemplate: null
      });
      let editor = createEditor(editorConfig);
      const template = editor.getBehaviorTemplate();
      expect(template && template.toString()).toEqual('');
   });

    it('returns the template when it’s empty on an existing skill', () => {
      editorConfig.group = editorConfig.group.behaviorVersions[0].buildUpdatedGroupFor(editorConfig.group, {
        responseTemplate: ResponseTemplate.fromString('')
      });
      let editor = createEditor(editorConfig);
      const template = editor.getBehaviorTemplate();
      expect(template && template.toString()).toEqual('');
    });

    it('returns the default template on a new, empty skill', () => {
      let editor = createEditor(newSkillConfig);
      const template = editor.getBehaviorTemplate();
      expect(template && template.toString()).toEqual(BehaviorVersion.defaultActionProps().responseTemplate.toString());
    });
  });

  describe('render', () => {
    it("renders the normal editor when isDataType is false", () => {
      editorConfig.group = editorConfig.group.behaviorVersions[0].buildUpdatedGroupFor(editorConfig.group, {
        config: editorConfig.group.behaviorVersions[0].config.clone({
          isDataType: false
        })
      });
      const editor = createEditor(editorConfig);
      const dataSpy = jest.spyOn(editor, 'renderDataTypeBehavior');
      const normalSpy = jest.spyOn(editor, 'renderNormalBehavior');
      editor.render();
      expect(dataSpy).not.toBeCalled();
      expect(normalSpy).toBeCalled();
    });
    it("renders the data type editor when isDataType is true", () => {
      editorConfig.group = editorConfig.group.behaviorVersions[0].buildUpdatedGroupFor(editorConfig.group, {
        config: editorConfig.group.behaviorVersions[0].config.clone({
          isDataType: true,
          dataTypeConfig: DataTypeConfig.fromJson({ fields: [], usesCode: true })
        })
      });
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
      editorConfig.group = editorConfig.group.clone({
        createdAt: Number(new Date()) - HALF_MINUTE
      });
      let editor = createEditor(editorConfig);
      editor.isModified = jest.fn(() => true);
      let justSaved = editor.isJustSaved();
      expect(justSaved).toEqual(false);
    });

    it("false if not a recent save", () => {
      editorConfig.group = editorConfig.group.clone({
        createdAt: Number(new Date()) - TWO_MINUTES
      });
      let editor = createEditor(editorConfig);
      editor.isModified = jest.fn(() => false);
      let justSaved = editor.isJustSaved();
      expect(justSaved).toEqual(false);
    });

    it("true if a recent save and isModified() is false", () => {
      editorConfig.group = editorConfig.group.clone({
        createdAt: Number(new Date()) - HALF_MINUTE
      });
      let editor = createEditor(editorConfig);
      editor.isModified = jest.fn(() => false);
      let justSaved = editor.isJustSaved();
      expect(justSaved).toEqual(true);
    });
  });

  describe('getOtherSavedInputsInGroup', () => {
    it("returns the (unique by inputId) saved params", () => {
      const groupId = editorConfig.group.id;
      const inputId = "abcd12345";
      const savedAnswerInput: Input = Input.fromJson({
        name: 'foo',
        question: '',
        paramType: editorConfig.builtinParamTypes[0],
        isSavedForTeam: false,
        isSavedForUser: true,
        inputId: inputId
      });
      const otherBehaviorsInGroup: Array<BehaviorVersion> = [
          {
            teamId: "1",
            behaviorId: "2",
            functionBody: "",
            responseTemplate: "",
            inputIds: [inputId],
            triggers: [],
            config: {
              isDataType: false,
              isTest: false,
              responseTypeId: normalResponseType
            },
            groupId: groupId
          },
          {
            teamId: "1",
            behaviorId: "3",
            functionBody: "",
            responseTemplate: "",
            inputIds: [inputId],
            triggers: [],
            config: {
              isDataType: false,
              isTest: false,
              responseTypeId: normalResponseType
            },
            groupId: groupId
          }
        ].map(BehaviorVersion.fromJson);
      editorConfig.group = editorConfig.group.clone({
        actionInputs: [savedAnswerInput],
        behaviorVersions: editorConfig.group.behaviorVersions.concat(otherBehaviorsInGroup)
      });
      const editor = createEditor(editorConfig);
      expect(editor.getOtherSavedInputsInGroup().map(ea => ea.inputId)).toEqual([otherBehaviorsInGroup[0].inputIds[0]]);
    });
  });

  describe('setBehaviorConfigProps', () => {
    it("clones the existing behavior config with updated properties", () => {
      editorConfig.group = editorConfig.group.clone({
        behaviorVersions: [editorConfig.group.behaviorVersions[0].clone({
          config: editorConfig.group.behaviorVersions[0].config.clone({
            responseTypeId: normalResponseType
          })
        })]
      });
      let editor = createEditor(editorConfig);
      const config = editor.getBehaviorConfig();
      expect(config && config.responseTypeId).toBe(normalResponseType);
      editor.setBehaviorConfigProps({
        responseTypeId: privateResponseType
      });
      const config2 = editor.getBehaviorConfig();
      expect(config2 && config2.responseTypeId).toEqual(privateResponseType);
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
