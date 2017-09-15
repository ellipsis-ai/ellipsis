jest.mock('../app/assets/javascripts/behavior_editor/code_editor')
  .mock('../app/assets/javascripts/shared_ui/react-codemirror');
window.crypto = require('./mocks/mock_window_crypto');
global.fetch = require('./mocks/mock_fetch');

import React from 'react';
import TestUtils from 'react-addons-test-utils';
const BehaviorEditor = require('../app/assets/javascripts/behavior_editor/index');
const BehaviorVersion = require('../app/assets/javascripts/models/behavior_version');
const BehaviorGroup = require('../app/assets/javascripts/models/behavior_group');
const ResponseTemplate = require('../app/assets/javascripts/models/response_template');
const ParamType = require('../app/assets/javascripts/models/param_type');

jsRoutes.controllers.BehaviorEditorController.edit = jest.fn(() => ({ url: '/mock_edit' }));
jsRoutes.controllers.BehaviorEditorController.save = jest.fn(() => ({ url: '/mock_save' }));
jsRoutes.controllers.BehaviorEditorController.newGroup = jest.fn(() => ({ url: '/mock_new_skill' }));
jsRoutes.controllers.ApplicationController.deleteBehaviorGroups = jest.fn(() => ({ url: '/mock_delete_behavior_group' }));
jsRoutes.controllers.BehaviorEditorController.edit = jest.fn(() => ({ url: '/mock_edit' }));
jsRoutes.controllers.BehaviorEditorController.nodeModuleVersionsFor = jest.fn(() => ({ url: '/mock_node_module_versions_for' }));


describe('BehaviorEditor', () => {
  const defaultConfig = Object.freeze({
    teamId: "A",
    group: {
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
            dataTypeConfig: {
              fields: [],
              name: ''
            }
          },
          knownEnvVarsUsed: [],
          groupId: '1',
          shouldRevealCodeEditor: true
        }
      ],
      libraryVersions: [],
      nodeModuleVersions: []
    },
    selectedId: "1",
    csrfToken: "2",
    envVariables: [ { name: "HOT_DOG" } ],
    builtinParamTypes: [{
      id: 'Text',
      name: 'Text',
      needsConfig: false
    }, {
      id: 'Number',
      name: 'Number',
      needsConfig: false
    }],
    oAuth2Applications: [{
      applicationId: "567890",
      displayName: "My awesome oauth app",
      keyName: "myAwesomeOauthApp"
    }, {
      applicationId: "098765",
      displayName: "My other awesome oauth app",
      keyName: "myOtherAwesomeOauthApp"
    }],
    linkedOAuth2ApplicationIds: [],
    shouldRevealCodeEditor: true,
    onSave: jest.fn(),
    savedAnswers: [],
    onForgetSavedAnswerForInput: jest.fn(),
    userId: "1"
  });

  const newSkillConfig = Object.freeze({
    "containerId": "editorContainer",
    "csrfToken": "1234",
    "group": {
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
        "config": { "isDataType": false },
        "knownEnvVarsUsed": []
      }],
      "libraryVersions": [],
      "requiredOAuth2ApiConfigs": [],
      "requiredSimpleTokenApis": [],
      "createdAt": "2017-09-15T11:58:07.36-04:00",
      "author": { "id": "3", "name": "attaboy" }
    },
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
    "oauth2Applications": [{
      "apiId": "RdG2Wm5DR0m2_4FZXf-yKA",
      "applicationId": "Yy1QcMTcT96tZZmUoYLroQ",
      "scope": "https://www.googleapis.com/auth/calendar",
      "displayName": "Google Calendar",
      "keyName": "googleCalendar"
    }],
    "oauth2Apis": [{
      "apiId": "RdG2Wm5DR0m2_4FZXf-yKA",
      "name": "Google",
      "requiresAuth": true,
      "newApplicationUrl": "https://console.developers.google.com/apis",
      "scopeDocumentationUrl": "https://developers.google.com/identity/protocols/googlescopes"
    }],
    "simpleTokenApis": [{
      "apiId": "pivotal-tracker",
      "name": "Pivotal Tracker",
      "tokenUrl": "https://www.pivotaltracker.com/profile",
      "logoImageUrl": "/assets/images/logos/pivotal_tracker.png"
    }],
    "linkedOAuth2ApplicationIds": ["Yy1QcMTcT96tZZmUoYLroQ"],
    "userId": "3",
    selectedId: "2",
    onSave: jest.fn(),
    onForgetSavedAnswerForInput: jest.fn()
  });

  let editorConfig;
  let firstBehavior;

  beforeEach(function() {
    editorConfig = Object.assign({}, defaultConfig);
    firstBehavior = editorConfig.group.behaviorVersions[0];
  });

  function createEditor(config) {
    const props = Object.assign({}, config, {
      group: BehaviorGroup.fromJson(config.group),
      builtinParamTypes: config.builtinParamTypes.map(ParamType.fromJson)
    });
    return TestUtils.renderIntoDocument(
      <BehaviorEditor {...props} />
    ).refs.component;
  }

  describe('getFunctionBody', () => {
    it('returns the defined function', () => {
      firstBehavior.functionBody = 'return;';
      let editor = createEditor(editorConfig);
      expect(editor.getFunctionBody()).toEqual('return;');
    });

    it('returns a string even when no function is defined', () => {
      delete firstBehavior.functionBody;
      firstBehavior.shouldRevealCodeEditor = false;
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
        inputVersionId: "xzy321",
        exportId: null
      }];
      firstBehavior.inputIds = editorConfig.group.actionInputs.map(ea => ea.inputId);
      let editor = createEditor(editorConfig);
      expect(editor.getInputs()).toEqual(editorConfig.group.actionInputs);
    });

    it('returns an array even when no params are defined', () => {
      delete firstBehavior.inputIds;
      let editor = createEditor(editorConfig);
      expect(editor.getInputs()).toEqual([]);
    });
  });

  describe('getBehaviorTemplate', () => {
    it('returns the template the defined template when it’s non-empty', () => {
      firstBehavior.responseTemplate = 'clowncar';
      let editor = createEditor(editorConfig);
      expect(editor.getBehaviorTemplate().toString()).toEqual('clowncar');
    });

    it('returns a default template when no template is defined', () => {
      delete firstBehavior.responseTemplate;
      let editor = createEditor(editorConfig);
      editor.getDefaultBehaviorTemplate = jest.fn();
      editor.getDefaultBehaviorTemplate.mockReturnValue(ResponseTemplate.fromString('default'));
      expect(editor.getBehaviorTemplate().toString()).toEqual('default');
    });

    it('returns a default template when the template is blank', () => {
      firstBehavior.responseTemplate = '';
      let editor = createEditor(editorConfig);
      editor.getDefaultBehaviorTemplate = jest.fn();
      editor.getDefaultBehaviorTemplate.mockReturnValue(ResponseTemplate.fromString('default'));
      expect(editor.getBehaviorTemplate().toString()).toEqual('default');
    });

    it('returns the original template when it has been modified', () => {
      firstBehavior.responseTemplate = '';
      let editor = createEditor(editorConfig);
      editor.hasModifiedTemplate = jest.fn();
      editor.hasModifiedTemplate.mockReturnValue(true);
      expect(editor.getBehaviorTemplate().toString()).toEqual('');
    });
  });

  describe('checkDataAndCallback', () => {
    it('sets the default template when that\'s all there is', () => {
      firstBehavior.responseTemplate = '';
      let editor = createEditor(editorConfig);
      let defaultTemplate = ResponseTemplate.fromString('default');
      editor.getDefaultBehaviorTemplate = jest.fn();
      editor.getDefaultBehaviorTemplate.mockReturnValue(defaultTemplate);
      editor.setEditableProp = jest.fn();
      let callback = jest.fn();
      editor.checkDataAndCallback(callback);
      let mock = editor.setEditableProp.mock;
      expect(mock.calls.length).toBe(1);
      let firstCallArgs = mock.calls[0];
      expect(firstCallArgs).toEqual(['responseTemplate', defaultTemplate, callback]);
    });
  });

  describe('onInputEnterKey', () => {
    it('focuses on the next param if there is one', () => {
      editorConfig.group.actionInputs = [{
        name: 'param1', question: 'What am I?', paramType: editorConfig.builtinParamTypes[0], inputId: "abc123"
      }, {
        name: 'param2', question: 'Who are you?', paramType: editorConfig.builtinParamTypes[0], inputId: "abc124"
      }];
      firstBehavior.inputIds = editorConfig.group.actionInputs.map(ea => ea.inputId);
      const editor = createEditor(editorConfig);
      editor.focusOnInputIndex = jest.fn();
      editor.addNewInput = jest.fn();
      editor.onInputEnterKey(0);
      expect(editor.focusOnInputIndex.mock.calls[0][0]).toBe(1);
      expect(editor.addNewInput.mock.calls.length).toBe(0);
    });

    it('adds a param if this is the last one and it has a question', () => {
      editorConfig.group.actionInputs = [{
        name: 'param1', question: 'What am I?', paramType: editorConfig.builtinParamTypes[0], inputId: "abc123"
      }, {
        name: 'param2', question: 'Who are you?', paramType: editorConfig.builtinParamTypes[0], inputId: "abc124"
      }];
      firstBehavior.inputIds = editorConfig.group.actionInputs.map(ea => ea.inputId);
      const editor = createEditor(editorConfig);
      editor.focusOnInputIndex = jest.fn();
      editor.addNewInput = jest.fn();
      editor.onInputEnterKey(1);
      expect(editor.focusOnInputIndex.mock.calls.length).toBe(0);
      expect(editor.addNewInput.mock.calls.length).toBe(1);
    });

    it('does nothing if this is the last one and has no question', () => {
      editorConfig.group.actionInputs = [{
        name: 'param1', question: 'What am I?', paramType: editorConfig.builtinParamTypes[0], inputId: "abc123"
      }, {
        name: 'param2', question: '', paramType: editorConfig.builtinParamTypes[0], inputId: "def456"
      }];
      firstBehavior.inputIds = editorConfig.group.actionInputs.map(ea => ea.inputId);
      const editor = createEditor(editorConfig);
      editor.focusOnInputIndex = jest.fn();
      editor.addNewInput = jest.fn();
      editor.onInputEnterKey(1);
      expect(editor.focusOnInputIndex.mock.calls.length).toBe(0);
      expect(editor.addNewInput.mock.calls.length).toBe(0);
    });
  });

  describe('updateTemplate', () => {
    it('sets a callback to mark the template as modified', () => {
      let editor = createEditor(editorConfig);
      editor.setEditableProp = jest.fn();
      editor.setState = jest.fn();
      editor.updateTemplate('new template');
      const callback = editor.setEditableProp.mock.calls[0][2];
      callback();
      expect(editor.setState).toBeCalledWith({ hasModifiedTemplate: true });
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
      const bv = editorConfig.group.behaviorVersions[0];
      bv.config.isDataType = true;
      bv.dataTypeConfig = { fields: [] };
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
        group: Object.assign({}, editorConfig.group, { createdAt: new Date() - HALF_MINUTE })
      });
      let editor = createEditor(config);
      editor.isModified = jest.fn(() => true);
      let justSaved = editor.isJustSaved();
      expect(justSaved).toEqual(false);
    });

    it("false if not a recent save", () => {
      let config = Object.assign({}, editorConfig, {
        group: Object.assign({}, editorConfig.group, { createdAt: new Date() - TWO_MINUTES })
      });
      let editor = createEditor(config);
      editor.isModified = jest.fn(() => false);
      let justSaved = editor.isJustSaved();
      expect(justSaved).toEqual(false);
    });

    it("true if a recent save and isModified() is false", () => {
      let config = Object.assign({}, editorConfig, {
        group: Object.assign({}, editorConfig.group, { createdAt: new Date() - HALF_MINUTE })
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
      const otherBehaviorsInGroup = [
          {
            behaviorId: "2",
            functionBody: "",
            responseTemplate: "",
            inputIds: [savedAnswerInput.inputId],
            triggers: [],
            config: {},
            knownEnvVarsUsed: [],
            groupId: groupId
          },
          {
            behaviorId: "3",
            functionBody: "",
            responseTemplate: "",
            inputIds: [savedAnswerInput.inputId],
            triggers: [],
            config: {},
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
});
