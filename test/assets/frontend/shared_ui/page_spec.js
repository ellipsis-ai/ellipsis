// @flow
import * as React from 'react';
import TestUtils from 'react-addons-test-utils';
import type {PageRequiredProps} from '../../../../app/assets/frontend/shared_ui/page';

// TODO: remove `any` when we're using ES6 modules
import Page from '../../../../app/assets/frontend/shared_ui/page';
import PageFooterRenderingError from '../../../../app/assets/frontend/shared_ui/page_footer_rendering_error';
import FixedFooter from '../../../../app/assets/frontend/shared_ui/fixed_footer';

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
  onRenderPanel: jest.fn(),
  onRenderNavItems: jest.fn(),
  onRenderNavActions: jest.fn(),
  footerHeight: 0
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

  describe('toggleActivePanel', () => {
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

    it('finds the active modal if a panel is toggled on modally', () => {
      const page = createMockedPage();
      const fooElement = document.createElement('div');
      page.onRenderPanel('foo', fooElement);
      const spy = jest.spyOn(page, 'getActiveModalElement');
      page.toggleActivePanel('foo', true);
      expect(spy).toHaveBeenCalledWith('foo');
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
  });

});
