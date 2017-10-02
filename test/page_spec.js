import React from 'react';
import TestUtils from 'react-addons-test-utils';

const Page = require('../app/assets/javascripts/shared_ui/page');

describe('Page', () => {
  const defaultComponentProps = {
    activePanelName: "",
    activePanelIsModal: false,
    onToggleActivePanel: jest.fn(),
    onClearActivePanel: jest.fn()
  };

  const SomeComponent = React.createClass({
    displayName: "SomeComponent",
    propTypes: Object.assign({}, Page.requiredPropTypes),
    getDefaultProps: function() {
      return Page.requiredPropDefaults();
    },
    render: function() {
      return (<div></div>);
    }
  });

  let props = {};

  beforeEach(() => {
    props = Object.assign({}, defaultComponentProps);
  });

  function createPage() {
    const feedbackContainer = document.createElement('span');
    return TestUtils.renderIntoDocument((
      <Page feedbackContainer={feedbackContainer}>
        <SomeComponent {...props} />
      </Page>
    ));
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

  describe('render', () => {
    it('renders an augmented inner component', () => {
      var page = createPage();
      var child = TestUtils.findRenderedComponentWithType(page, SomeComponent);
      expect(TestUtils.isCompositeComponentWithType(child, SomeComponent)).toBe(true);
      expect(child.props.onToggleActivePanel).toBe(page.toggleActivePanel);
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
