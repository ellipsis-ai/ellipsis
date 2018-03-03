import * as React from 'react';
import * as ReactDOM from 'react-dom';

export type NavItemContent = {
  title: string,
  url?: string | null,
  callback?: (() => void) | null
}

export interface PageRequiredProps {
  activePanelName: string,
  activePanelIsModal: boolean,
  onToggleActivePanel: (name: string, beModal?: boolean, optionalCallback?: () => void) => void,
  onClearActivePanel: (optionalCallback?: () => void) => void,
  onRenderFooter: (content?: any, footerClassName?: string) => any,
  onRenderNavItems: (items: Array<NavItemContent>) => void,
  onRenderNavActions: (content) => void,
  onRenderPanel: (panelName: string, panel) => void,
  footerHeight: number,
  ref?: any
};

import Event from '../lib/event';
import FeedbackPanel from '../panels/feedback';
import Collapsible from './collapsible';
import FixedFooter from './fixed_footer';
import ModalScrim from './modal_scrim';
import Button from '../form/button';
import NavItem from './nav_item';
import autobind from '../lib/autobind';
import PageFooterRenderingError from './page_footer_rendering_error';

type Props = {
  onRender: <P extends PageRequiredProps>(pageProps: PageRequiredProps) => React.ReactElement<P>,
  csrfToken: string,
  feedbackContainer?: HTMLElement | null
}

type State = {
  activePanelName: string,
  activePanelIsModal: boolean,
  previousPanelName: string,
  previousPanelIsModal: boolean,
  footerHeight: number
}

class Page extends React.Component<Props, State> {
    panels: { [prop: string]: any };
    footer: any;
    component: any;
    navItems: HTMLElement | null;
    navActions: HTMLElement | null;
    static requiredPropTypes: {};
    static feedbackContainerId: string;

    constructor(props: Props) {
      super(props);
      autobind(this);
      this.state = this.getDefaultState();
      this.footer = null;
      this.panels = {};
      this.navItems = document.getElementById("mainNavItems");
      this.navActions = document.getElementById("mainNavActions");
    }

    getDefaultState(): State {
      return {
        activePanelName: "",
        activePanelIsModal: false,
        previousPanelName: "",
        previousPanelIsModal: false,
        footerHeight: 0
      };
    }

    toggleActivePanel(name: string, beModal?: boolean, optionalCallback?: () => void): void {
      var alreadyOpen = this.state.activePanelName === name;
      var callback = typeof(optionalCallback) === 'function' ?
        optionalCallback : (() => {
          if (!alreadyOpen && beModal) {
            var activeModal = this.getActiveModalElement(name);
            if (activeModal) {
              this.focusOnPrimaryOrFirstPossibleElement(activeModal);
            }
          }
        });
      this.setState({
        activePanelName: alreadyOpen ? this.state.previousPanelName : name,
        activePanelIsModal: alreadyOpen ? this.state.previousPanelIsModal : !!beModal,
        previousPanelName: this.state.activePanelName,
        previousPanelIsModal: this.state.activePanelIsModal
      }, callback);
    }

    clearActivePanel(optionalCallback?: () => void): void {
      this.setState(this.getDefaultState(), optionalCallback);
    }

    onRenderPanel(panelName: string, panel): void {
      const newPanel = {};
      newPanel[panelName] = panel;
      this.panels = Object.assign({}, this.panels, newPanel);
    }

    handleEscKey(): void {
      if (this.state.activePanelName) {
        this.clearActivePanel();
      }
    }

    onDocumentKeyDown(event: any): void {
      if (Event.keyPressWasEsc(event)) {
        this.handleEscKey();
      }
    }

    handleModalFocus(event: any): void {
      var activeModal = this.state.activePanelIsModal ? this.getActiveModalElement(this.state.activePanelName) : null;
      if (!activeModal) {
        return;
      }
      var focusTarget = event.target;
      var possibleMatches = activeModal.getElementsByTagName(focusTarget.tagName);
      var match = Array.prototype.some.call(possibleMatches, function(element) {
        return element === focusTarget;
      });
      if (!match) {
        event.preventDefault();
        event.stopImmediatePropagation();
        this.focusOnFirstPossibleElement(activeModal);
      }
    }

    getActiveModalElement(panelName: string): any {
      const panel = this.panels[panelName];
      return panel ? ReactDOM.findDOMNode(panel) : null;
    }

    focusOnPrimaryOrFirstPossibleElement(parentElement: any): void {
      var primaryElement = parentElement.querySelector('button.button-primary');
      if (primaryElement) {
        primaryElement.focus();
      } else {
        this.focusOnFirstPossibleElement(parentElement);
      }
    }

    focusOnFirstPossibleElement(parentElement: any): void {
      var tabSelector = 'a[href], area[href], input:not([disabled]), button:not([disabled]), select:not([disabled]), textarea:not([disabled]), iframe, object, embed, *[tabindex], *[contenteditable]';
      var firstFocusableElement = parentElement.querySelector(tabSelector);
      if (firstFocusableElement) {
        firstFocusableElement.focus();
      }
    }

    componentDidMount(): void {
      window.document.addEventListener('keydown', this.onDocumentKeyDown, false);
      window.document.addEventListener('focus', this.handleModalFocus, true);
      const container = this.props.feedbackContainer || document.getElementById(Page.feedbackContainerId);
      if (this.footer && container) {
        ReactDOM.render(this.renderFeedbackLink(), container);
      } else {
        throw new PageFooterRenderingError(this);
      }
    }

    getFooterHeight(): number {
      return this.state.footerHeight;
    }

    setFooterHeight(number: number): void {
      this.setState({
        footerHeight: number
      });
    }

    toggleFeedback(): void {
      this.toggleActivePanel('feedback', true);
    }

    onRenderFooter(content?, footerClassName?: string) {
      return (
        <div>
          <ModalScrim isActive={this.state.activePanelIsModal} onClick={this.clearActivePanel} />
          <FixedFooter ref={(el) => this.footer = el} className={`bg-white ${footerClassName || ""}`} onHeightChange={this.setFooterHeight}>
            <Collapsible revealWhen={this.state.activePanelName === 'feedback'}>
              <FeedbackPanel onDone={this.toggleFeedback} csrfToken={this.props.csrfToken} />
            </Collapsible>
            {content}
          </FixedFooter>
        </div>
      );
    }

    onRenderNavItems(navItems: Array<NavItemContent>) {
      // This should use ReactDOM.createPortal when we upgrade to React 16
      const el = this.navItems;
      if (el) {
        ReactDOM.render((
          <div className="columns">
            {navItems.map((ea, index) => (
              <NavItem key={`navItem${index}`} title={ea.title} url={ea.url} callback={ea.callback} />
            ))}
          </div>
        ), el);
      }
    }

    onRenderNavActions(content) {
      // This should use ReactDOM.createPortal when we upgrade to React 16
      const el = this.navActions;
      if (el) {
        ReactDOM.render((
          <div>{content}</div>
        ), el);
      }
    }

    renderFeedbackLink() {
      return (
        <Button className="button-dropdown-item plm" onClick={this.toggleFeedback}>Feedback</Button>
      );
    }

    render() {
      return (
        <div className="flex-row-cascade">
          {this.props.onRender({
            activePanelName: this.state.activePanelName,
            activePanelIsModal: this.state.activePanelIsModal,
            onToggleActivePanel: this.toggleActivePanel,
            onClearActivePanel: this.clearActivePanel,
            onRenderFooter: this.onRenderFooter,
            onRenderPanel: this.onRenderPanel,
            onRenderNavItems: this.onRenderNavItems,
            onRenderNavActions: this.onRenderNavActions,
            footerHeight: this.getFooterHeight(),
            ref: (component) => this.component = component
          })}
        </div>
      );
    }

    static requiredPropDefaults(): PageRequiredProps {
      return {
        activePanelName: "",
        activePanelIsModal: false,
        onToggleActivePanel: Page.placeholderCallback,
        onClearActivePanel: Page.placeholderCallback,
        onRenderFooter: Page.placeholderCallback,
        onRenderPanel: Page.placeholderCallback,
        onRenderNavItems: Page.placeholderCallback,
        onRenderNavActions: Page.placeholderCallback,
        footerHeight: 0
      };
    }

    static placeholderCallback() {
      void(0);
    }
}

Page.requiredPropTypes = {
  activePanelName: React.PropTypes.string.isRequired,
  activePanelIsModal: React.PropTypes.bool.isRequired,
  onToggleActivePanel: React.PropTypes.func.isRequired,
  onClearActivePanel: React.PropTypes.func.isRequired,
  onRenderFooter: React.PropTypes.func.isRequired,
  onRenderPanel: React.PropTypes.func.isRequired,
  onRenderNavItems: React.PropTypes.func.isRequired,
  onRenderNavActions: React.PropTypes.func.isRequired,
  footerHeight: React.PropTypes.number.isRequired
};

Page.feedbackContainerId = 'header-feedback';

export default Page;

