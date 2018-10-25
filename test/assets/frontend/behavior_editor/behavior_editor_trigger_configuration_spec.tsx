import * as React from 'react';
import * as TestUtils from 'react-addons-test-utils';
import TriggerConfiguration, {TriggerConfigurationProps} from '../../../../app/assets/frontend/behavior_editor/trigger_configuration';
import Trigger, {TriggerType} from '../../../../app/assets/frontend/models/trigger';
import ParamNotInFunctionNotificationData from "../../../../app/assets/frontend/models/notifications/param_not_in_function_notification_data";
import InvalidParamInTriggerNotificationData from "../../../../app/assets/frontend/models/notifications/invalid_param_in_trigger_notification_data";

jest.mock('emoji-mart/css/emoji-mart.css', () => '');

describe('TriggerConfiguration', () => {
  const defaultConfig: TriggerConfigurationProps = {
    triggers: Trigger.triggersFromJson([{
      text: "Do the tests run?",
      requiresMention: false,
      isRegex: false,
      caseSensitive: false,
      triggerType: TriggerType.MessageSent
    }]),
    inputNames: [],
    onToggleHelp: jest.fn(),
    helpVisible: false,
    onTriggerAdd: jest.fn(),
    onTriggerChange: jest.fn(),
    onTriggerDelete: jest.fn(),
    onTriggerDropdownToggle: jest.fn(),
    onAddNewInput: jest.fn(),
    openDropdownName: ""
  };

  function getDefaultConfig(): TriggerConfigurationProps {
    return Object.assign({}, defaultConfig);
  }

  function create(config: TriggerConfigurationProps) {
    return TestUtils.renderIntoDocument(
      <TriggerConfiguration {...config} />
    ) as TriggerConfiguration;
  }

  describe('onMessageTriggerEnterKey', () => {
    it('focuses on the next param if there is one', () => {
      const triggerConfig = getDefaultConfig();
      triggerConfig.triggers = Trigger.triggersFromJson([{
        text: "trigger1",
        requiresMention: false,
        isRegex: false,
        caseSensitive: false,
        triggerType: TriggerType.MessageSent
      }, {
        text: "trigger2",
        requiresMention: false,
        isRegex: false,
        caseSensitive: false,
        triggerType: TriggerType.MessageSent
      }]);
      const component = create(triggerConfig);
      const mockFocusOnMessageTriggerIndex = jest.fn();
      component.focusOnMessageTriggerIndex = mockFocusOnMessageTriggerIndex;
      const mockAddMessageTrigger = jest.fn();
      component.addMessageTrigger = mockAddMessageTrigger;
      component.onMessageTriggerEnterKey(0);
      expect(mockFocusOnMessageTriggerIndex.mock.calls[0][0]).toBe(1);
      expect(mockAddMessageTrigger.mock.calls.length).toBe(0);
    });

    it('adds a trigger if this is the last one and it has text', () => {
      const triggerConfig = getDefaultConfig();
      triggerConfig.triggers = Trigger.triggersFromJson([{
        text: "trigger1",
        requiresMention: false,
        isRegex: false,
        caseSensitive: false,
        triggerType: TriggerType.MessageSent
      }, {
        text: "trigger2",
        requiresMention: false,
        isRegex: false,
        caseSensitive: false,
        triggerType: TriggerType.MessageSent
      }]);
      const component = create(triggerConfig);
      const mockFocusOnMessageTriggerIndex = jest.fn();
      component.focusOnMessageTriggerIndex = mockFocusOnMessageTriggerIndex;
      const mockAddMessageTrigger = jest.fn();
      component.addMessageTrigger = mockAddMessageTrigger;
      component.onMessageTriggerEnterKey(1);
      expect(mockFocusOnMessageTriggerIndex.mock.calls.length).toBe(0);
      expect(mockAddMessageTrigger.mock.calls.length).toBe(1);
    });

    it('does nothing if this is the last one and has no text', () => {
      const triggerConfig = getDefaultConfig();
      triggerConfig.triggers = Trigger.triggersFromJson([{
        text: "trigger1",
        requiresMention: false,
        isRegex: false,
        caseSensitive: false,
        triggerType: TriggerType.MessageSent
      }, {
        text: "",
        requiresMention: false,
        isRegex: false,
        caseSensitive: false,
        triggerType: TriggerType.MessageSent
      }]);
      const component = create(triggerConfig);
      const mockFocusOnMessageTriggerIndex = jest.fn();
      component.focusOnMessageTriggerIndex = mockFocusOnMessageTriggerIndex;
      const mockAddMessageTrigger = jest.fn();
      component.addMessageTrigger = mockAddMessageTrigger;
      component.onMessageTriggerEnterKey(1);
      expect(mockFocusOnMessageTriggerIndex.mock.calls.length).toBe(0);
      expect(mockAddMessageTrigger.mock.calls.length).toBe(0);
    });
  });

  describe('hasPrimaryTrigger', () => {
    it('returns false when the first trigger is missing', () => {
      const triggerConfig = getDefaultConfig();
      triggerConfig.triggers = [];
      const editor = create(triggerConfig);
      expect(editor.hasPrimaryTrigger()).toBe(false);
    });
    it('returns false when the first trigger is empty', () => {
      const triggerConfig = getDefaultConfig();
      triggerConfig.triggers = Trigger.triggersFromJson([{
        text: "",
        requiresMention: false,
        isRegex: false,
        caseSensitive: false,
        triggerType: TriggerType.MessageSent
      }]);
      const component = create(triggerConfig);
      expect(component.hasPrimaryTrigger()).toBe(false);
    });
    it('returns true when the first trigger has text', () => {
      const triggerConfig = getDefaultConfig();
      triggerConfig.triggers = Trigger.triggersFromJson([{
        text: "sudo make me a sandwich",
        requiresMention: false,
        isRegex: false,
        caseSensitive: false,
        triggerType: TriggerType.MessageSent
      }]);
      const component = create(triggerConfig);
      expect(component.hasPrimaryTrigger()).toBe(true);
    });
  });

  describe('getNotificationsFor', () => {
    it('returns an empty array when there are no params in the triggers', () => {
      const triggerConfig = getDefaultConfig();
      const editor = create(triggerConfig);
      expect(editor.getNotificationsFor(triggerConfig.triggers[0])).toEqual([]);
    });

    it('returns a param_not_in_function when a trigger contains an unknown input', () => {
      const triggerConfig = getDefaultConfig();
      triggerConfig.triggers = Trigger.triggersFromJson([{
        text: "be {adjective}",
        requiresMention: false,
        isRegex: false,
        caseSensitive: false,
        triggerType: TriggerType.MessageSent
      }]);
      const component = create(triggerConfig);
      const notifications = component.getNotificationsFor(triggerConfig.triggers[0]);
      expect(notifications.length).toBe(1);
      expect(notifications[0]).toBeInstanceOf(ParamNotInFunctionNotificationData);
      expect(notifications[0]).toEqual(expect.objectContaining({
        kind: "param_not_in_function",
        name: "adjective",
        onClick: expect.any(Function)
      }));
    });

    it('returns a invalid_param_in_trigger when a trigger contains an invalid input label', () => {
      const triggerConfig = getDefaultConfig();
      triggerConfig.triggers = Trigger.triggersFromJson([{
        text: "be {%afraid*}",
        requiresMention: false,
        isRegex: false,
        caseSensitive: false,
        triggerType: TriggerType.MessageSent
      }]);
      const component = create(triggerConfig);
      const notifications = component.getNotificationsFor(triggerConfig.triggers[0]);
      expect(notifications.length).toBe(1);
      expect(notifications[0]).toBeInstanceOf(InvalidParamInTriggerNotificationData);
      expect(notifications[0]).toEqual(expect.objectContaining({
        kind: "invalid_param_in_trigger",
        name: "%afraid*"
      }));
    });
  });

});
