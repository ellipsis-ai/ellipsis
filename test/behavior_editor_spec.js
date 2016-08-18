jest.unmock('../app/assets/javascripts/behavior_editor/index');

import React from 'react';
import TestUtils from 'react-addons-test-utils';
const BehaviorEditor = require('../app/assets/javascripts/behavior_editor/index');

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
    oAuth2Applications: [{
      applicationId: "567890",
      displayName: "My awesome oauth app",
      keyName: "myAwesomeOauthApp"
    }, {
      applicationId: "098765",
      displayName: "My other awesome oauth app",
      keyName: "myOtherAwesomeOauthApp"
    }],
    notifications: [],
    shouldRevealCodeEditor: true
  };

  let editorConfig = {};

  beforeEach(function() {
    editorConfig = Object.assign(editorConfig, defaultConfig);
  });

  function createEditor(config) {
    return TestUtils.renderIntoDocument(
      <BehaviorEditor {...config} />
    );
  }

  describe('getBehaviorTriggers', () => {
    it('returns the defined triggers', () => {
      editorConfig.triggers = [{ text: 'bang', requiresMention: false, isRegex: false, caseSensitive: false }];
      let editor = createEditor(editorConfig);
      expect(editor.getBehaviorTriggers()).toEqual([{ text: 'bang', requiresMention: false, isRegex: false, caseSensitive: false }]);
    });

    it('returns getInitialTriggers when no triggers are defined', () => {
      delete editorConfig.triggers;
      let editor = createEditor(editorConfig);
      editor.getInitialTriggers = jest.fn();
      editor.getInitialTriggers.mockReturnValue([{ text: '', requiresMention: false, isRegex: false, caseSensitive: false }]);
      expect(editor.getBehaviorTriggers()).toEqual([{ text: '', requiresMention: false, isRegex: false, caseSensitive: false }]);
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
      editorConfig.params = [{ name: 'clown', question: 'what drives the car?' }];
      let editor = createEditor(editorConfig);
      expect(editor.getBehaviorParams()).toEqual([{ name: 'clown', question: 'what drives the car?' }]);
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
      expect(editor.getBehaviorTemplate()).toEqual('clowncar');
    });

    it('returns a default template when no template is defined', () => {
      delete editorConfig.responseTemplate;
      let editor = createEditor(editorConfig);
      editor.getDefaultBehaviorTemplate = jest.fn();
      editor.getDefaultBehaviorTemplate.mockReturnValue('default');
      expect(editor.getBehaviorTemplate()).toEqual('default');
    });

    it('returns a default template when the template is blank', () => {
      editorConfig.responseTemplate = '';
      let editor = createEditor(editorConfig);
      editor.getDefaultBehaviorTemplate = jest.fn();
      editor.getDefaultBehaviorTemplate.mockReturnValue('default');
      expect(editor.getBehaviorTemplate()).toBeTruthy('default');
    });

    it('returns the original template when it has been modified', () => {
      editorConfig.responseTemplate = '';
      let editor = createEditor(editorConfig);
      editor.hasModifiedTemplate = jest.fn();
      editor.hasModifiedTemplate.mockReturnValue(true);
      expect(editor.getBehaviorTemplate()).toEqual('');
    });

    it('submits default template when that\'s all there is', () => {
      editorConfig.responseTemplate = '';
      let editor = createEditor(editorConfig);
      editor.getDefaultBehaviorTemplate = jest.fn();
      editor.getDefaultBehaviorTemplate.mockReturnValue('default');
      editor.refs.behaviorForm.submit = jest.fn();
      editor.setBehaviorProp = jest.fn((key, value, callback) => callback());
      const event = {
        preventDefault: jest.fn()
      };
      editor.onSaveClick(event);
      expect(event.preventDefault.mock.calls.length).toBe(1);
      expect(editor.setBehaviorProp.mock.calls.length).toBe(1);
      expect(editor.setBehaviorProp.mock.calls[0][0]).toBe('responseTemplate');
      expect(editor.setBehaviorProp.mock.calls[0][1]).toBe('default');
      expect(editor.refs.behaviorForm.submit.mock.calls.length).toBe(1);

    });
  });

  describe('onParamEnterKey', () => {
    it('focuses on the next param if there is one', () => {
      editorConfig.params = [{
        name: 'param1', question: 'What am I?'
      }, {
        name: 'param2', question: 'Who are you?'
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
        name: 'param1', question: 'What am I?'
      }, {
        name: 'param2', question: 'Who are you?'
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
        name: 'param1', question: 'What am I?'
      }, {
        name: 'param2', question: ''
      }];
      const editor = createEditor(editorConfig);
      editor.focusOnParamIndex = jest.fn();
      editor.addParam = jest.fn();
      editor.onParamEnterKey(1);
      expect(editor.focusOnParamIndex.mock.calls.length).toBe(0);
      expect(editor.addParam.mock.calls.length).toBe(0);
    });
  });

  describe('onTriggerEnterKey', () => {
    it('focuses on the next param if there is one', () => {
      editorConfig.triggers = [{
        text: "trigger1",
        requiresMention: false,
        isRegex: false,
        caseSensitive: false
      }, {
        text: "trigger2",
        requiresMention: false,
        isRegex: false,
        caseSensitive: false
      }];
      const editor = createEditor(editorConfig);
      editor.focusOnTriggerIndex = jest.fn();
      editor.addTrigger = jest.fn();
      editor.onTriggerEnterKey(0);
      expect(editor.focusOnTriggerIndex.mock.calls[0][0]).toBe(1);
      expect(editor.addTrigger.mock.calls.length).toBe(0);
    });

    it('adds a trigger if this is the last one and it has text', () => {
      editorConfig.triggers = [{
        text: "trigger1",
        requiresMention: false,
        isRegex: false,
        caseSensitive: false
      }, {
        text: "trigger2",
        requiresMention: false,
        isRegex: false,
        caseSensitive: false
      }];
      const editor = createEditor(editorConfig);
      editor.focusOnTriggerIndex = jest.fn();
      editor.addTrigger = jest.fn();
      editor.onTriggerEnterKey(1);
      expect(editor.focusOnTriggerIndex.mock.calls.length).toBe(0);
      expect(editor.addTrigger.mock.calls.length).toBe(1);
    });

    it('does nothing if this is the last one and has no text', () => {
      editorConfig.triggers = [{
        text: "trigger1",
        requiresMention: false,
        isRegex: false,
        caseSensitive: false
      }, {
        text: "",
        requiresMention: false,
        isRegex: false,
        caseSensitive: false
      }];
      const editor = createEditor(editorConfig);
      editor.focusOnTriggerIndex = jest.fn();
      editor.addTrigger = jest.fn();
      editor.onTriggerEnterKey(1);
      expect(editor.focusOnTriggerIndex.mock.calls.length).toBe(0);
      expect(editor.addTrigger.mock.calls.length).toBe(0);
    });
  });

  describe('hasPrimaryTrigger', () => {
    it('returns false when the first trigger is missing', () => {
      editorConfig.triggers = [];
      const editor = createEditor(editorConfig);
      expect(editor.hasPrimaryTrigger()).toBe(false);
    });
    it('returns false when the first trigger is empty', () => {
      editorConfig.triggers = [{
        text: "",
        requiresMention: false,
        isRegex: false,
        caseSensitive: false
      }];
      const editor = createEditor(editorConfig);
      expect(editor.hasPrimaryTrigger()).toBe(false);
    });
    it('returns true when the first trigger has text', () => {
      editorConfig.triggers = [{
        text: "sudo make me a sandwich",
        requiresMention: false,
        isRegex: false,
        caseSensitive: false
      }];
      const editor = createEditor(editorConfig);
      expect(editor.hasPrimaryTrigger()).toBe(true);
    });
  });

  describe('hasCalledOnError', () => {
    it('returns true when the code includes onError called with a string', () => {
      editorConfig.functionBody = 'var f = "b";\nonError("this is an error");';
      const editor = createEditor(editorConfig);
      expect(editor.hasCalledOnError()).toBe(true);
    });
    it('returns false when the code includes onError called with nothing', () => {
      editorConfig.functionBody = "var f = 'b';\nonError();";
      const editor = createEditor(editorConfig);
      expect(editor.hasCalledOnError()).toBe(false);
    });
    it('returns true when the code includes ellipsis.error called with a string', () => {
      editorConfig.functionBody = 'var f = "b";\nellipsis.error("this is an error");';
      const editor = createEditor(editorConfig);
      expect(editor.hasCalledOnError()).toBe(true);
    });
    it('returns false when the code includes ellipsis.error called with nothing', () => {
      editorConfig.functionBody = "var f = 'b';\nellipsis.error();";
      const editor = createEditor(editorConfig);
      expect(editor.hasCalledOnError()).toBe(false);
    });
    it('returns false when the code doesn’t include onError', () => {
      editorConfig.functionBody = 'var f = "b";';
      const editor = createEditor(editorConfig);
      expect(editor.hasCalledOnError()).toBe(false);
    });
  });

  describe('hasCalledOnSuccess', () => {
    it('returns true when the code includes onSuccess called with something', () => {
      editorConfig.functionBody = 'var f = "b";\nonSuccess(f);';
      const editor = createEditor(editorConfig);
      expect(editor.hasCalledOnSuccess()).toBe(true);
    });
    it('returns true when the code includes onSuccess called with nothing', () => {
      editorConfig.functionBody = 'var f = "b";\nonSuccess();';
      const editor = createEditor(editorConfig);
      expect(editor.hasCalledOnSuccess()).toBe(true);
    });
    it('returns true when the code includes ellipsis.success called with something', () => {
      editorConfig.functionBody = 'var f = "b";\nellipsis.success(f);';
      const editor = createEditor(editorConfig);
      expect(editor.hasCalledOnSuccess()).toBe(true);
    });
    it('returns true when the code includes ellipsis.success called with nothing', () => {
      editorConfig.functionBody = 'var f = "b";\nellipsis.success();';
      const editor = createEditor(editorConfig);
      expect(editor.hasCalledOnSuccess()).toBe(true);
    });
    it('returns false when the code doesn’t include onSuccess', () => {
      editorConfig.functionBody = 'var f = "b";';
      const editor = createEditor(editorConfig);
      expect(editor.hasCalledOnSuccess()).toBe(false);
    });
  });

  describe('hasCalledNoResponse', () => {
    it('returns true when the code includes noResponse with nothing', () => {
      editorConfig.functionBody = 'var f = "b";\nellipsis.noResponse();';
      const editor = createEditor(editorConfig);
      expect(editor.hasCalledNoResponse()).toBe(true);
    });
    it('returns false when the code doesn’t include noResponse', () => {
      editorConfig.functionBody = 'var f = "b";';
      const editor = createEditor(editorConfig);
      expect(editor.hasCalledNoResponse()).toBe(false);
    });
  });

  describe('hasCalledRequire', () => {
    it('returns true when the code calls require with something', () => {
      editorConfig.functionBody = 'var Intl = require("intl");\nIntl.NumberFormat().format();';
      const editor = createEditor(editorConfig);
      expect(editor.hasCalledRequire()).toBe(true);
    });
    it('returns false when the code calls require with nothing', () => {
      editorConfig.functionBody = 'var Intl = require();\nIntl.NumberFormat().format();';
      const editor = createEditor(editorConfig);
      expect(editor.hasCalledRequire()).toBe(false);
    });
    it('returns false when the code doesn’t call require', () => {
      editorConfig.functionBody = 'var f = "b";';
      const editor = createEditor(editorConfig);
      expect(editor.hasCalledRequire()).toBe(false);
    });
  });
});
