jest.unmock('../app/assets/javascripts/behavior_editor/trigger_configuration');

import React from 'react';
import TestUtils from 'react-addons-test-utils';
const TriggerConfiguration = require('../app/assets/javascripts/behavior_editor/trigger_configuration');

describe('TriggerConfiguration', () => {
  const defaultConfig = {
    isFinishedBehavior: false,
    triggers: [{
      text: "Do the tests run?",
      requiresMention: false,
      isRegex: false,
      caseSensitive: false
    }],
    onToggleHelp: jest.fn(),
    helpVisible: false,
    onTriggerAdd: jest.fn(),
    onTriggerChange: jest.fn(),
    onTriggerDelete: jest.fn()
  };

  let triggerConfig = {};

  beforeEach(function() {
    triggerConfig = Object.assign(triggerConfig, defaultConfig);
  });

  function createEditor(config) {
    return TestUtils.renderIntoDocument(
      <TriggerConfiguration {...config} />
    );
  }

  describe('onTriggerEnterKey', () => {
    it('focuses on the next param if there is one', () => {
      triggerConfig.triggers = [{
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
      const editor = createEditor(triggerConfig);
      editor.focusOnTriggerIndex = jest.fn();
      editor.addTrigger = jest.fn();
      editor.onTriggerEnterKey(0);
      expect(editor.focusOnTriggerIndex.mock.calls[0][0]).toBe(1);
      expect(editor.addTrigger.mock.calls.length).toBe(0);
    });

    it('adds a trigger if this is the last one and it has text', () => {
      triggerConfig.triggers = [{
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
      const editor = createEditor(triggerConfig);
      editor.focusOnTriggerIndex = jest.fn();
      editor.addTrigger = jest.fn();
      editor.onTriggerEnterKey(1);
      expect(editor.focusOnTriggerIndex.mock.calls.length).toBe(0);
      expect(editor.addTrigger.mock.calls.length).toBe(1);
    });

    it('does nothing if this is the last one and has no text', () => {
      triggerConfig.triggers = [{
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
      const editor = createEditor(triggerConfig);
      editor.focusOnTriggerIndex = jest.fn();
      editor.addTrigger = jest.fn();
      editor.onTriggerEnterKey(1);
      expect(editor.focusOnTriggerIndex.mock.calls.length).toBe(0);
      expect(editor.addTrigger.mock.calls.length).toBe(0);
    });
  });

  describe('hasPrimaryTrigger', () => {
    it('returns false when the first trigger is missing', () => {
      triggerConfig.triggers = [];
      const editor = createEditor(triggerConfig);
      expect(editor.hasPrimaryTrigger()).toBe(false);
    });
    it('returns false when the first trigger is empty', () => {
      triggerConfig.triggers = [{
        text: "",
        requiresMention: false,
        isRegex: false,
        caseSensitive: false
      }];
      const editor = createEditor(triggerConfig);
      expect(editor.hasPrimaryTrigger()).toBe(false);
    });
    it('returns true when the first trigger has text', () => {
      triggerConfig.triggers = [{
        text: "sudo make me a sandwich",
        requiresMention: false,
        isRegex: false,
        caseSensitive: false
      }];
      const editor = createEditor(triggerConfig);
      expect(editor.hasPrimaryTrigger()).toBe(true);
    });
  });

});
