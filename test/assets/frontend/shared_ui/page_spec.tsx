import * as React from 'react';
import * as TestUtils from 'react-dom/test-utils';

import Page, {PageRequiredProps} from '../../../../app/assets/frontend/shared_ui/page';
import PageFooterRenderingError from '../../../../app/assets/frontend/shared_ui/page_footer_rendering_error';
import FixedFooter from '../../../../app/assets/frontend/shared_ui/fixed_footer';

const renderPlaceholder = (ea: any) => ea;
function placeholderCallback() {
  void(0);
}

function getPageRequiredProps(): PageRequiredProps {
  return {
    activePanelName: "",
    activePanelIsModal: false,
    onToggleActivePanel: placeholderCallback,
    onClearActivePanel: placeholderCallback,
    onRenderFooter: renderPlaceholder,
    onRenderPanel: renderPlaceholder,
    onRenderNavItems: renderPlaceholder,
    onRenderNavActions: renderPlaceholder,
    headerHeight: 0,
    footerHeight: 0
  };
};

export {getPageRequiredProps};

class FooterRenderingComponent extends React.Component<{
  onRenderFooter: (content: any) => void
}> {
  render() {
    return (
      <div>
        <span/>
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

class NoFooterComponent extends React.Component {
  render() {
    return (
      <div />
    );
  }
}

describe('Page', () => {
  function createPage(override?: any): Page {
    const feedbackContainer = document.createElement('span');
    return TestUtils.renderIntoDocument((
      <Page feedbackContainer={feedbackContainer} csrfToken={"1"}
        onRender={override || ((pageProps) => (
          <FooterRenderingComponent {...FooterRenderingComponentDefaultProps} {...pageProps} />
        ))}
      />
    )) as Page;
  }

  function createMockedPage() {
    return createPage();
  }

  function setStateMockFactory() {
    return jest.fn((newState, providedCallback) => {
      if (providedCallback) {
        providedCallback();
      }
    });
  }

  describe('render', () => {
    it('renders an augmented inner component', () => {
      var page = createPage();
      var child = TestUtils.findRenderedComponentWithType(page, FooterRenderingComponent as React.ComponentClass<any>);
      expect(child).toBeDefined();
      expect(child && child.props.onToggleActivePanel).toBe(page.toggleActivePanel);
    });
  });

  describe('onRenderFooter', () => {
    it('when called by the child component, it renders a FixedFooter', () => {
      const page = createPage();
      const footer = TestUtils.findRenderedComponentWithType(page, FixedFooter as React.ComponentClass<any>);
      expect(footer).toBeDefined();
    });

    it('when not called by the child component, it throws a PageFooterRenderingError', () => {
      expect.assertions(1);
      try {
        createPage(((pageProps: PageRequiredProps) => (<NoFooterComponent {...pageProps} />)));
      } catch(e) {
        expect(e).toBeInstanceOf(PageFooterRenderingError);
      }
    });
  });

  describe('toggleActivePanel', () => {
    it('passes a callback to setState if a valid function is passed', () => {
      const page = createMockedPage();
      const callback = jest.fn();
      const setStateMock = setStateMockFactory();
      page.setState = setStateMock;
      page.toggleActivePanel('foo', false, callback);
      expect(setStateMock.mock.calls[0][1]).toBe(callback);
      expect(callback).toBeCalled();
    });

    it('doesn’t pass the callback to setState if it’s not a function', () => {
      const page = createMockedPage();
      const notACallback = undefined;
      const setStateMock = setStateMockFactory();
      page.setState = setStateMock;
      page.toggleActivePanel('foo', false, notACallback);
      expect(setStateMock.mock.calls[0][1]).not.toBe(notACallback);
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
      const setStateMock = setStateMockFactory();
      page.setState = setStateMock;
      page.clearActivePanel(callback);
      expect(setStateMock.mock.calls[0][1]).toBe(callback);
      expect(callback).toBeCalled();
    });
  });

});
