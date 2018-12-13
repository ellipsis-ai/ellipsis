import * as React from 'react';
import * as TestUtils from 'react-addons-test-utils';
import DropdownMenu, {
  DropdownMenuItemProps,
  DropdownMenuProps
} from '../../../../app/assets/frontend/shared_ui/dropdown_menu';

describe('DropdownMenu', () => {
  let toggled = false;

  const defaultConfig: Partial<DropdownMenuProps> = {
    labelClassName: '',
    label: 'A menu',
    openWhen: false,
    menuClassName: '',
    toggle: function() { toggled = !toggled; }
  };

  const defaultItemConfig: DropdownMenuItemProps = {
    onClick: function() { return; },
    label: "item label"
  };

  const defaultEventData = {
    stopPropagation: jest.fn()
  };

  function createDropdown(config: DropdownMenuProps, itemConfig: DropdownMenuItemProps) {
    return TestUtils.renderIntoDocument(
      <DropdownMenu {...config}>
        <DropdownMenu.Item {...itemConfig} />
      </DropdownMenu>
    ) as DropdownMenu;
  }

  let config: DropdownMenuProps;
  let itemConfig: DropdownMenuItemProps;
  let eventData: {
    stopPropagation: () => void
  };

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
      if (dropdown.button) {
        TestUtils.Simulate.mouseDown(dropdown.button, eventData);
        TestUtils.Simulate.mouseUp(dropdown.button, eventData);
      }
      expect(toggled).toBe(true);
    });
  });

  describe('onClick', () => {
    it('stops clicks on the label from bubbling up', () => {
      const dropdown = createDropdown(config, itemConfig);
      var onContainerClick = jest.fn();
      // We have to use an actual DOM event here, and add a real listener for it
      var evt = new Event("click", {"bubbles": true, "cancelable": true});
      if (dropdown.container) {
        dropdown.container.addEventListener('click', onContainerClick, false);
      }
      if (dropdown.button) {
        dropdown.button.dispatchEvent(evt);
      }
      expect(onContainerClick.mock.calls.length).toBe(0);
    });
  });

  describe('mousedown on menu, mouseup on item', () => {
    it('fires the item’s onclick handler and re-hides the menu', () => {
      const onClick = jest.fn();
      itemConfig.onClick = onClick;
      const dropdown = createDropdown(config, itemConfig);
      const itemButton = TestUtils.findRenderedDOMComponentWithClass(dropdown, 'button-dropdown-item');
      dropdown.onItemMouseUp = jest.fn();
      expect(toggled).toBe(false);
      if (dropdown.button) {
        TestUtils.Simulate.mouseDown(dropdown.button, eventData);
      }
      expect(toggled).toBe(true);
      TestUtils.Simulate.mouseUp(itemButton, eventData);
      expect(toggled).toBe(false);
      expect(onClick.mock.calls.length).toBe(1);
    });
  });
});
