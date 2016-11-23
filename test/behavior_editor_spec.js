jest
  .unmock('../app/assets/javascripts/behavior_editor/index')
  .unmock('../app/assets/javascripts/models/behavior_version')
  .unmock('../app/assets/javascripts/models/param')
  .unmock('../app/assets/javascripts/models/response_template')
  .unmock('../app/assets/javascripts/models/trigger')
  .unmock('../app/assets/javascripts/sort');

import React from 'react';
import TestUtils from 'react-addons-test-utils';
const BehaviorEditor = require('../app/assets/javascripts/behavior_editor/index');
const BehaviorVersion = require('../app/assets/javascripts/models/behavior_version');
const ResponseTemplate = require('../app/assets/javascripts/models/response_template');
const Trigger = require('../app/assets/javascripts/models/trigger');

jsRoutes.controllers.BehaviorEditorController.save = jest.fn(() => ({ url: '/mock_save' }));

describe('BehaviorEditor', () => {
  const defaultConfig = {
    teamId: "A",
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
    csrfToken: "2",
    justSaved: false,
    envVariables: [ { name: "HOT_DOG" } ],
    paramTypes: [{
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
    groupId: '1',
    otherBehaviorsInGroup: []
  };

  let editorConfig;

  beforeEach(function() {
    editorConfig = Object.assign({}, defaultConfig);
  });

  function createEditor(config) {
    const behaviorVersionConfig = Object.assign({}, BehaviorVersion.fromJson(config));
    return TestUtils.renderIntoDocument(
      <BehaviorEditor {...behaviorVersionConfig} />
    );
  }

  describe('getInitialTriggersFromProps', () => {
    it('returns the defined triggers', () => {
      editorConfig.triggers = [{ text: 'bang', requiresMention: false, isRegex: false, caseSensitive: false }];
      let editor = createEditor(editorConfig);
      expect(editor.getInitialTriggersFromProps(editor.props)).toEqual([{ text: 'bang', requiresMention: false, isRegex: false, caseSensitive: false }]);
    });

    it('returns a single blank trigger when no triggers are defined', () => {
      delete editorConfig.triggers;
      let editor = createEditor(editorConfig);
      expect(editor.getInitialTriggersFromProps(editor.props)).toEqual([new Trigger()]);
    });
  });

  describe('getBehaviorFunctionBody', () => {
    it('returns the defined function', () => {
      editorConfig.functionBody = 'return;';
      let editor = createEditor(editorConfig);
      expect(editor.getBehaviorFunctionBody()).toEqual('return;');
    });

    it('returns a string even when no function is defined', () => {
      delete editorConfig.functionBody;
      editorConfig.shouldRevealCodeEditor = false;
      let editor = createEditor(editorConfig);
      expect(editor.getBehaviorFunctionBody()).toEqual("");
    });
  });

  describe('getBehaviorParams', () => {
    it('returns the defined parameters', () => {
      editorConfig.params = [{ name: 'clown', question: 'what drives the car?', paramType: editorConfig.paramTypes[0], isSavedForTeam: false, isSavedForUser: true, inputId: "abcd1234" }];
      let editor = createEditor(editorConfig);
      expect(editor.getBehaviorParams()).toEqual(editorConfig.params);
    });

    it('returns an array even when no params are defined', () => {
      delete editorConfig.params;
      let editor = createEditor(editorConfig);
      expect(editor.getBehaviorParams()).toEqual([]);
    });
  });

  describe('getBehaviorTemplate', () => {
    it('returns the template the defined template when it’s non-empty', () => {
      editorConfig.responseTemplate = 'clowncar';
      let editor = createEditor(editorConfig);
      expect(editor.getBehaviorTemplate().toString()).toEqual('clowncar');
    });

    it('returns a default template when no template is defined', () => {
      delete editorConfig.responseTemplate;
      let editor = createEditor(editorConfig);
      editor.getDefaultBehaviorTemplate = jest.fn();
      editor.getDefaultBehaviorTemplate.mockReturnValue(ResponseTemplate.fromString('default'));
      expect(editor.getBehaviorTemplate().toString()).toEqual('default');
    });

    it('returns a default template when the template is blank', () => {
      editorConfig.responseTemplate = '';
      let editor = createEditor(editorConfig);
      editor.getDefaultBehaviorTemplate = jest.fn();
      editor.getDefaultBehaviorTemplate.mockReturnValue(ResponseTemplate.fromString('default'));
      expect(editor.getBehaviorTemplate().toString()).toEqual('default');
    });

    it('returns the original template when it has been modified', () => {
      editorConfig.responseTemplate = '';
      let editor = createEditor(editorConfig);
      editor.hasModifiedTemplate = jest.fn();
      editor.hasModifiedTemplate.mockReturnValue(true);
      expect(editor.getBehaviorTemplate().toString()).toEqual('');
    });
  });

  describe('checkDataAndCallback', () => {
    it('sets the default template when that\'s all there is', () => {
      editorConfig.responseTemplate = '';
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
      editorConfig.params = [{
        name: 'param1', question: 'What am I?', paramType: editorConfig.paramTypes[0]
      }, {
        name: 'param2', question: 'Who are you?', paramType: editorConfig.paramTypes[0]
      }];
      const editor = createEditor(editorConfig);
      editor.focusOnParamIndex = jest.fn();
      editor.addParam = jest.fn();
      editor.onParamEnterKey(0);
      expect(editor.focusOnParamIndex.mock.calls[0][0]).toBe(1);
      expect(editor.addParam.mock.calls.length).toBe(0);
    });

    it('adds a param if this is the last one and it has a question', () => {
      editorConfig.params = [{
        name: 'param1', question: 'What am I?', paramType: editorConfig.paramTypes[0]
      }, {
        name: 'param2', question: 'Who are you?', paramType: editorConfig.paramTypes[0]
      }];
      const editor = createEditor(editorConfig);
      editor.focusOnParamIndex = jest.fn();
      editor.addParam = jest.fn();
      editor.onParamEnterKey(1);
      expect(editor.focusOnParamIndex.mock.calls.length).toBe(0);
      expect(editor.addParam.mock.calls.length).toBe(1);
    });

    it('does nothing if this is the last one and has no question', () => {
      editorConfig.params = [{
        name: 'param1', question: 'What am I?', paramType: editorConfig.paramTypes[0]
      }, {
        name: 'param2', question: '', paramType: editorConfig.paramTypes[0]
      }];
      const editor = createEditor(editorConfig);
      editor.focusOnParamIndex = jest.fn();
      editor.addParam = jest.fn();
      editor.onParamEnterKey(1);
      expect(editor.focusOnParamIndex.mock.calls.length).toBe(0);
      expect(editor.addParam.mock.calls.length).toBe(0);
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
    it("renders the normal editor when there's no dataTypeName property", () => {
      editorConfig.config.dataTypeName = null;
      let editor = createEditor(editorConfig);
      editor.renderDataTypeBehavior = jest.fn();
      editor.renderNormalBehavior = jest.fn();
      editor.render();
      expect(editor.renderDataTypeBehavior).not.toBeCalled();
      expect(editor.renderNormalBehavior).toBeCalled();
    });
    it("renders the data type editor when there's a dataTypeName property", () => {
      editorConfig.config.dataTypeName = 'My pretend data type';
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
      expect(newParam.paramType).toEqual(editorConfig.paramTypes[0]);
    });

    it("creates a new parameter with other attributes as desired", () => {
      let editor = createEditor(editorConfig);
      let newParam = editor.createNewParam({ name: "clownCar", question: "how did twitter propel itself?" });
      expect(newParam.name).toEqual("clownCar");
      expect(newParam.question).toEqual("how did twitter propel itself?");
    });
  });
});
