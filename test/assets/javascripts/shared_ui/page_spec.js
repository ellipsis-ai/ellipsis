// @flow
import * as React from 'react';
import TestUtils from 'react-addons-test-utils';
import type {PageRequiredProps} from '../../../../app/assets/javascripts/shared_ui/page';

// TODO: remove `any` when we're using ES6 modules
const Page: any = require('../../../../app/assets/javascripts/shared_ui/page');
const PageFooterRenderingError = require('../../../../app/assets/javascripts/shared_ui/page_footer_rendering_error');
const FixedFooter = require('../../../../app/assets/javascripts/shared_ui/fixed_footer');

class FooterRenderingComponent extends React.Component<PageRequiredProps> {
  render() {
    return (
      <div>
        {this.props.onRenderFooter(null)}
      </div>
    );
  }
}

const FooterRenderingComponentDefaultProps = Object.freeze({
  activePanelName: "",
  activePanelIsModal: false,
  onToggleActivePanel: jest.fn(),
  onClearActivePanel: jest.fn(),
  onRenderFooter: jest.fn(),
  onGetFooterHeight: jest.fn()
});

class NoFooterComponent extends React.Component<{}> {
  render() {
    return (
      <div />
    );
  }
}

describe('Page', () => {
  function createPage(overrideComponent) {
    const feedbackContainer = document.createElement('span');
    return TestUtils.renderIntoDocument((
      <Page feedbackContainer={feedbackContainer} csrfToken={"1"}>
        {overrideComponent || (
          <FooterRenderingComponent {...FooterRenderingComponentDefaultProps} />
        )}
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
      var child = TestUtils.findRenderedComponentWithType(page, FooterRenderingComponent);
      expect(child).toBeDefined();
      expect(child && child.props.onToggleActivePanel).toBe(page.toggleActivePanel);
    });
  });

  describe('onRenderFooter', () => {
    it('when called by the child component, it renders a FixedFooter', () => {
      const page = createPage();
      const footer = TestUtils.findRenderedComponentWithType(page, FixedFooter);
      expect(footer).toBeDefined();
    });

    it('when not called by the child component, it throws a PageFooterRenderingError', () => {
      expect.assertions(1);
      try {
        createPage((
          <NoFooterComponent/>
        ));
      } catch(e) {
        expect(e).toBeInstanceOf(PageFooterRenderingError);
      }
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
