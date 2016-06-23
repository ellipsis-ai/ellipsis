jest.unmock('../app/assets/javascripts/behavior_editor_dropdown_menu');

import React from 'react';
import ReactDOM from 'react-dom';
import TestUtils from 'react-addons-test-utils';
const BehaviorEditor = require('../app/assets/javascripts/behavior_editor_dropdown_menu');
const BehaviorEditorDropdownMenu = require('../app/assets/javascripts/behavior_editor_dropdown_menu');

describe('BehaviorEditorDropdownMenu', () => {
  let toggled = false;

  const defaultConfig = {
    labelClassName: '',
    label: 'A menu',
    openWhen: false,
    menuClassName: '',
    toggle: function() { toggled = !toggled; }
  };

  const defaultItemConfig = {
    onClick: function() {},
    label: "item label"
  };

  const defaultEventData = {
    stopPropagation: jest.fn()
  };

  function createDropdown(config, itemConfig) {
    return TestUtils.renderIntoDocument(
      <BehaviorEditorDropdownMenu {...config}>
        <BehaviorEditorDropdownMenu.Item {...itemConfig} />
      </BehaviorEditorDropdownMenu>
    );
  }

  let config = {};
  let itemConfig = {};
  let eventData = {};

  beforeEach(() => {
    toggled = false;
    config = Object.assign(config, defaultConfig);
    eventData = Object.assign(eventData, defaultEventData);
    itemConfig = Object.assign(itemConfig, defaultItemConfig);
  });

  describe('onMouseDown', () => {
    it('toggles the menu when clicking on the label', () => {
      const dropdown = createDropdown(config, itemConfig);
      expect(toggled).toBe(false);
      TestUtils.Simulate.mouseDown(dropdown.refs.button, eventData);
      TestUtils.Simulate.mouseUp(dropdown.refs.button, eventData);
      expect(toggled).toBe(true);
    });
  });

  describe('onClick', () => {
    it('stops clicks on the label from bubbling up', () => {
      const dropdown = createDropdown(config, itemConfig);
      var onContainerClick = jest.fn();
      // We have to use an actual DOM event here, and add a real listener for it
      var evt = new Event("click", {"bubbles": true, "cancelable": true});
      dropdown.refs.container.addEventListener('click', onContainerClick, false);
      dropdown.refs.button.dispatchEvent(evt);
      expect(onContainerClick.mock.calls.length).toBe(0);
    });
  });

  describe('mousedown on menu, mouseup on item', () => {
    it('fires the item’s onclick handler and re-hides the menu', () => {
      itemConfig.onClick = jest.fn();
      const dropdown = createDropdown(config, itemConfig);
      const item = dropdown.refs.menuItem0;
      const itemButton = TestUtils.findRenderedDOMComponentWithClass(dropdown, 'button-dropdown-item');
      dropdown.onItemMouseUp = jest.fn();
      expect(toggled).toBe(false);
      TestUtils.Simulate.mouseDown(dropdown.refs.button, eventData);
      expect(toggled).toBe(true);
      TestUtils.Simulate.mouseUp(itemButton, eventData);
      expect(toggled).toBe(false);
      expect(itemConfig.onClick.mock.calls.length).toBe(1);
    });
  });
});
