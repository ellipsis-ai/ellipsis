import React from 'react';
import TestUtils from 'react-addons-test-utils';

const PageWithPanels = require('../app/assets/javascripts/shared_ui/page_with_panels');

describe('PageWithPanels', () => {
  const defaultComponentProps = {
    activePanelName: "",
    activePanelIsModal: false,
    onToggleActivePanel: jest.fn(),
    onClearActivePanel: jest.fn()
  };

  const InnerComponent = React.createClass({
    displayName: "SomeComponent",
    propTypes: PageWithPanels.requiredPropTypes(),
    render: function() {
      return (<div></div>);
    }
  });

  const OuterComponent = PageWithPanels.with(InnerComponent);

  let props;

  beforeEach(() => {
    props = Object.assign({}, defaultComponentProps);
  });

  function createPage() {
    return TestUtils.renderIntoDocument(<OuterComponent props={props}/>);
  }

  function createMockedPage() {
    const page = createPage();
    page.setState = jest.fn((newState, providedCallback) => {
      if (providedCallback) {
        providedCallback();
      }
    });
    return page;
  }

  describe('with', () => {
    it('returns a wrapped component', () => {
      var page = createPage();
      var child = TestUtils.findRenderedComponentWithType(page, InnerComponent);
      expect(TestUtils.isCompositeComponentWithType(child, InnerComponent)).toBe(true);
    });
  });

  describe('toggleWithActivePanel', () => {
    it('passes a callback to setState if a valid function is passed', () => {
      const page = createMockedPage();
      const callback = jest.fn();
      page.toggleActivePanel('foo', false, callback);
      expect(page.setState.mock.calls[0][1]).toBe(callback);
      expect(callback).toBeCalled();
    });

    it('doesn’t pass the callback to setState if it’s not a function', () => {
      const page = createMockedPage();
      const notACallback = {};
      page.toggleActivePanel('foo', false, notACallback);
      expect(page.setState.mock.calls[0][1]).not.toBe(notACallback);
    });
  });

  describe('clearActivePanel', () => {
    it('passes a callback to setState if a valid function is passed', () => {
      const page = createMockedPage();
      const callback = jest.fn();
      page.clearActivePanel(callback);
      expect(page.setState.mock.calls[0][1]).toBe(callback);
      expect(callback).toBeCalled();
    });

    it('passes no callback to setState if no valid function is passed', () => {
      const page = createMockedPage();
      const notACallback = {};
      page.clearActivePanel(notACallback);
      expect(page.setState.mock.calls[0][1]).toBeFalsy();
    });
  });

});
