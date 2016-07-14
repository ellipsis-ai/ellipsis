jest.unmock('../app/assets/javascripts/behavior_editor/index');

import React from 'react';
import ReactDOM from 'react-dom';
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
    csrfToken: "2",
    justSaved: false,
    envVariableNames: ["HOT_DOG"],
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

});
