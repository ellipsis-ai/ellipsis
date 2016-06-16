jest.unmock('../app/assets/javascripts/behavior_editor');

import React from 'react';
import ReactDOM from 'react-dom';
import TestUtils from 'react-addons-test-utils';
var BehaviorEditor = require('../app/assets/javascripts/behavior_editor');

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
    csrfToken: "2",
    justSaved: false,
    envVariableNames: ["HOT_DOG"],
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

  describe('utils', () => {
    const array = ['a', 'b', 'c'];
    const obj = { a: 0, b: { c: 1 } };
    let editor;
    beforeEach(() => {
      editor = createEditor(defaultConfig);
    });

    describe('arrayWithNewElementAtIndex', () => {
      it('copies an array before modifying it', () => {
        const newArray = editor.utils.arrayWithNewElementAtIndex(array, 'z', 2);
        expect(array).toEqual(['a', 'b', 'c']);
        expect(newArray).toEqual(['a', 'b', 'z']);
      });
    });

    describe('arrayRemoveElementAtIndex', () => {
      it('copies an array before removing an element from it', () => {
        const newArray = editor.utils.arrayRemoveElementAtIndex(array, 2);
        expect(array).toEqual(['a', 'b', 'c']);
        expect(newArray).toEqual(['a', 'b']);
      });
    });

    describe('objectWithNewValueAtKey', () => {
      it('copies an object before modifying a property of it', () => {
        const newObj = editor.utils.objectWithNewValueAtKey(obj, 1, 'a');
        expect(obj).toEqual({ a: 0, b: { c: 1 } });
        expect(newObj).toEqual({ a: 1, b: { c: 1 } });
      });
    });
  });

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

  describe('resetURL', () => {
    let editor;

    beforeEach(() => {
      editor = createEditor(editorConfig);
      editor.getBrowserPathname = jest.fn();
      editor.getBrowserPathname.mockReturnValue('/edit_behavior/abcdef');
      editor.getBrowserQueryParams = jest.fn();
      editor.setBrowserURL = jest.fn();
    });

    it('removes justSaved=true when it’s the only query parameter', () => {
      editor.getBrowserQueryParams.mockReturnValue('?justSaved=true');
      editor.resetURL();
      expect(editor.setBrowserURL.mock.calls[0][0]).toEqual('/edit_behavior/abcdef');
    });

    it('removes justSaved=true but leaves trailing parameters', () => {
      editor.getBrowserQueryParams.mockReturnValue('?justSaved=true&sky=blue&earth=round');
      editor.resetURL();
      expect(editor.setBrowserURL.mock.calls[0][0]).toEqual('/edit_behavior/abcdef?sky=blue&earth=round');
    });

    it('removes justSaved=true but leaves leading parameters', () => {
      editor.getBrowserQueryParams.mockReturnValue('?sky=blue&earth=round&justSaved=true');
      editor.resetURL();
      expect(editor.setBrowserURL.mock.calls[0][0]).toEqual('/edit_behavior/abcdef?sky=blue&earth=round');
    });

    it('removes justSaved=true but leaves leading and trailing parameters', () => {
      editor.getBrowserQueryParams.mockReturnValue('?sky=blue&justSaved=true&earth=round');
      editor.resetURL();
      expect(editor.setBrowserURL.mock.calls[0][0]).toEqual('/edit_behavior/abcdef?sky=blue&earth=round');
    });

    it('doesn’t set the browser URL if justSaved=true isn’t a query parameter', () => {
      editor.getBrowserQueryParams.mockReturnValue('');
      editor.resetURL();
      expect(editor.setBrowserURL.mock.calls.length).toBe(0);
    });
  });
});
