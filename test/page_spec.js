import React from 'react';
import TestUtils from 'react-addons-test-utils';

const Page = require('../app/assets/javascripts/shared_ui/page');
const PageFooterRenderingError = require('../app/assets/javascripts/shared_ui/page_footer_rendering_error');
const FixedFooter = require('../app/assets/javascripts/shared_ui/fixed_footer');

describe('Page', () => {
  class FooterRenderingComponent extends React.Component {
    render() {
      return (
        <div>
          {this.props.onRenderFooter(null)}
        </div>
      );
    }
  }

  class NoFooterComponent extends React.Component {
    render() {
      return (
        <div />
      );
    }
  }

  FooterRenderingComponent.propTypes = Object.assign({}, Page.requiredPropTypes);
  FooterRenderingComponent.defaultProps = Page.requiredPropDefaults();

  function createPage(overrideComponent) {
    const feedbackContainer = document.createElement('span');
    return TestUtils.renderIntoDocument((
      <Page feedbackContainer={feedbackContainer} csrfToken={"1"}>
        {overrideComponent || (
          <FooterRenderingComponent/>
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
      expect(child.props.onToggleActivePanel).toBe(page.toggleActivePanel);
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
        const page = createPage((
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
