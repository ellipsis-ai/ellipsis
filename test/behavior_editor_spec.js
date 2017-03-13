jest.mock('../app/assets/javascripts/behavior_editor/code_editor')
  .mock('../app/assets/javascripts/shared_ui/react-codemirror');

import React from 'react';
import TestUtils from 'react-addons-test-utils';
const BehaviorEditor = require('../app/assets/javascripts/behavior_editor/index');
const BehaviorVersion = require('../app/assets/javascripts/models/behavior_version');
const BehaviorGroup = require('../app/assets/javascripts/models/behavior_group');
const ResponseTemplate = require('../app/assets/javascripts/models/response_template');

jsRoutes.controllers.BehaviorEditorController.save = jest.fn(() => ({ url: '/mock_save' }));
jsRoutes.controllers.BehaviorEditorController.newGroup = jest.fn(() => ({ url: '/mock_new_skill' }));
jsRoutes.controllers.ApplicationController.deleteBehaviorGroups = jest.fn(() => ({ url: '/mock_delete_behavior_group' }));

describe('BehaviorEditor', () => {
  const defaultConfig = {
    teamId: "A",
    group: {
      behaviorVersions: [
        {
          behaviorId: "1",
          functionBody: "onSuccess('Woot')",
          responseTemplate: "{successResult}",
          params: [],
          triggers: [{
            text: "Do the tests run?",
            requiresMention: false,
            isRegex: false,
            caseSensitive: false
          }],
          config: {},
          knownEnvVarsUsed: [],
          groupId: '1',
          shouldRevealCodeEditor: true
        }
      ]
    },
    selectedBehaviorId: "1",
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
    notifications: [],
    shouldRevealCodeEditor: true,
    onSave: jest.fn(),
    savedAnswers: [],
    onForgetSavedAnswerForInput: jest.fn()
  };

  let editorConfig;
  let firstBehavior;

  beforeEach(function() {
    editorConfig = Object.assign({}, defaultConfig);
    firstBehavior = editorConfig.group.behaviorVersions[0];
  });

  function createEditor(config) {
    const props = Object.assign({}, config, {
      group: BehaviorGroup.fromJson(config.group)
    });
    return TestUtils.renderIntoDocument(
      <BehaviorEditor {...props} />
    ).refs.component;
  }

  describe('getBehaviorFunctionBody', () => {
    it('returns the defined function', () => {
      firstBehavior.functionBody = 'return;';
      let editor = createEditor(editorConfig);
      expect(editor.getBehaviorFunctionBody()).toEqual('return;');
    });

    it('returns a string even when no function is defined', () => {
      delete firstBehavior.functionBody;
      firstBehavior.shouldRevealCodeEditor = false;
      let editor = createEditor(editorConfig);
      expect(editor.getBehaviorFunctionBody()).toEqual("");
    });
  });

  describe('getBehaviorParams', () => {
    it('returns the defined parameters', () => {
      firstBehavior.params = [{ name: 'clown', question: 'what drives the car?', paramType: editorConfig.builtinParamTypes[0], isSavedForTeam: false, isSavedForUser: true, inputId: "abcd1234", inputVersionId: "xzy321", inputExportId: null }];
      let editor = createEditor(editorConfig);
      expect(editor.getBehaviorParams()).toEqual(firstBehavior.params);
    });

    it('returns an array even when no params are defined', () => {
      delete firstBehavior.params;
      let editor = createEditor(editorConfig);
      expect(editor.getBehaviorParams()).toEqual([]);
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
      editor.setBehaviorProp = jest.fn();
      let callback = jest.fn();
      editor.checkDataAndCallback(callback);
      let mock = editor.setBehaviorProp.mock;
      expect(mock.calls.length).toBe(1);
      let firstCallArgs = mock.calls[0];
      expect(firstCallArgs).toEqual(['responseTemplate', defaultTemplate, callback]);
    });
  });

  describe('onParamEnterKey', () => {
    it('focuses on the next param if there is one', () => {
      firstBehavior.params = [{
        name: 'param1', question: 'What am I?', paramType: editorConfig.builtinParamTypes[0]
      }, {
        name: 'param2', question: 'Who are you?', paramType: editorConfig.builtinParamTypes[0]
      }];
      const editor = createEditor(editorConfig);
      editor.focusOnParamIndex = jest.fn();
      editor.addNewParam = jest.fn();
      editor.onParamEnterKey(0);
      expect(editor.focusOnParamIndex.mock.calls[0][0]).toBe(1);
      expect(editor.addNewParam.mock.calls.length).toBe(0);
    });

    it('adds a param if this is the last one and it has a question', () => {
      firstBehavior.params = [{
        name: 'param1', question: 'What am I?', paramType: editorConfig.builtinParamTypes[0]
      }, {
        name: 'param2', question: 'Who are you?', paramType: editorConfig.builtinParamTypes[0]
      }];
      const editor = createEditor(editorConfig);
      editor.focusOnParamIndex = jest.fn();
      editor.addNewParam = jest.fn();
      editor.onParamEnterKey(1);
      expect(editor.focusOnParamIndex.mock.calls.length).toBe(0);
      expect(editor.addNewParam.mock.calls.length).toBe(1);
    });

    it('does nothing if this is the last one and has no question', () => {
      firstBehavior.params = [{
        name: 'param1', question: 'What am I?', paramType: editorConfig.builtinParamTypes[0]
      }, {
        name: 'param2', question: '', paramType: editorConfig.builtinParamTypes[0]
      }];
      const editor = createEditor(editorConfig);
      editor.focusOnParamIndex = jest.fn();
      editor.addNewParam = jest.fn();
      editor.onParamEnterKey(1);
      expect(editor.focusOnParamIndex.mock.calls.length).toBe(0);
      expect(editor.addNewParam.mock.calls.length).toBe(0);
    });
  });

  describe('resetNotificationsImmediately', () => {
    const newNotifications = [{
      kind: "a", details: "new"
    }, {
      kind: "b", details: "new"
    }];
    const oldNotifications = [{
      kind: "b", details: "old"
    }, {
      kind: "c", details: "old"
    }, {
      kind: "d", details: "old", hidden: true
    }];
    it('concatenates new notifications with old, unneeded, still-visible ones', () => {
      let editor = createEditor(editorConfig);
      editor.buildNotifications = jest.fn(() => newNotifications);
      editor.getNotifications = jest.fn(() => oldNotifications);
      editor.setState = jest.fn();
      editor.resetNotificationsImmediately();
      expect(editor.setState).toBeCalledWith({
        notifications: [{
          kind: "a", details: "new"
        }, {
          kind: "b", details: "new"
        }, {
          kind: "c", details: "old", hidden: true
        }]
      });
    });
  });

  describe('updateTemplate', () => {
    it('sets a callback to mark the template as modified', () => {
      let editor = createEditor(editorConfig);
      editor.setBehaviorProp = jest.fn();
      editor.setState = jest.fn();
      editor.updateTemplate('new template');
      const callback = editor.setBehaviorProp.mock.calls[0][2];
      callback();
      expect(editor.setState).toBeCalledWith({ hasModifiedTemplate: true });
    });
  });

  describe('render', () => {
    it("renders the normal editor when isDataType is false", () => {
      firstBehavior.config.isDataType = false;
      let editor = createEditor(editorConfig);
      editor.renderDataTypeBehavior = jest.fn();
      editor.renderNormalBehavior = jest.fn();
      editor.render();
      expect(editor.renderDataTypeBehavior).not.toBeCalled();
      expect(editor.renderNormalBehavior).toBeCalled();
    });
    it("renders the data type editor when isDataType is true", () => {
      editorConfig.group.behaviorVersions[0].config.isDataType = true;
      let editor = createEditor(editorConfig);
      editor.renderDataTypeBehavior = jest.fn();
      editor.renderNormalBehavior = jest.fn();
      editor.render();
      expect(editor.renderDataTypeBehavior).toBeCalled();
      expect(editor.renderNormalBehavior).not.toBeCalled();
    });
  });

  describe('createNewParam', () => {
    it("creates a new parameter with the parameter type set to the first possible one", () => {
      let editor = createEditor(editorConfig);
      let newParam = editor.createNewParam();
      expect(newParam.paramType).toEqual(editorConfig.builtinParamTypes[0]);
    });

    it("creates a new parameter with other attributes as desired", () => {
      let editor = createEditor(editorConfig);
      let newParam = editor.createNewParam({ name: "clownCar", question: "how did twitter propel itself?" });
      expect(newParam.name).toEqual("clownCar");
      expect(newParam.question).toEqual("how did twitter propel itself?");
    });
  });

  describe('getOtherSavedParametersInGroup', () => {
    it("returns the (unique by inputId) saved params", () => {
      const groupId = editorConfig.group.id;
      const inputId = "abcd12345";
      const savedAnswerParam = {
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
            params: [savedAnswerParam],
            triggers: [],
            config: {},
            knownEnvVarsUsed: [],
            groupId: groupId
          },
          {
            behaviorId: "3",
            functionBody: "",
            responseTemplate: "",
            params: [savedAnswerParam],
            triggers: [],
            config: {},
            knownEnvVarsUsed: [],
            groupId: groupId
          }
        ].map(ea => BehaviorVersion.fromJson(ea));
      let allVersions = editorConfig.group.behaviorVersions.concat(otherBehaviorsInGroup);
      let newGroup = Object.assign({}, editorConfig.group, { behaviorVersions: allVersions });
      let config = Object.assign({}, editorConfig, { group: newGroup });
      let editor = createEditor(config);
      expect(editor.getOtherSavedParametersInGroup()).toEqual([otherBehaviorsInGroup[0].params[0]]);
    });
  });
});
