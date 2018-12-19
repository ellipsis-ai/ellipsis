import * as React from 'react';
import * as ReactDOM from 'react-dom';

export interface NavItemContent {
  title: string,
  url?: Option<string>,
  callback?: Option<() => void>
}

export interface PageRequiredProps {
  activePanelName: string,
  activePanelIsModal: boolean,
  onToggleActivePanel: (name: string, beModal?: boolean, optionalCallback?: () => void) => void,
  onClearActivePanel: (optionalCallback?: () => void) => void,
  onRenderFooter: (content?: any, footerClassName?: string) => any,
  onRenderNavItems: (items: Array<NavItemContent>) => void,
  onRenderNavActions: (content: React.ReactNode) => void,
  onRenderPanel: (panelName: string, panel: Container) => void,
  onRevealedPanel: () => void,
  headerHeight: number,
  footerHeight: number,
  ref?: any
}

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
  feedbackContainer?: Option<HTMLElement>
}

type State = {
  activePanelName: string,
  activePanelIsModal: boolean,
  previousPanelName: string,
  previousPanelIsModal: boolean,
  headerHeight: number,
  footerHeight: number
}

type Container = React.Component | HTMLElement | null;
type PanelMap = { [name: string]: Container };

class Page extends React.Component<Props, State> {
    panels: PanelMap;
    footer: Container;
    component: React.Component;
    header: Option<HTMLElement>;
    navItems: Option<HTMLElement>;
    navActions: Option<HTMLElement>;
    static feedbackContainerId: string;

    constructor(props: Props) {
      super(props);
      autobind(this);
      this.state = this.getDefaultState();
      this.footer = null;
      this.panels = {};
      this.navItems = document.getElementById("mainNavItems");
      this.navActions = document.getElementById("mainNavActions");
      this.header = document.getElementById("main-header");
    }

    getDefaultState(): State {
      return {
        activePanelName: "",
        activePanelIsModal: false,
        previousPanelName: "",
        previousPanelIsModal: false,
        headerHeight: this.state ? this.state.headerHeight : 0,
        footerHeight: this.state ? this.state.footerHeight : 0
      };
    }

    toggleActivePanel(name: string, beModal?: boolean, optionalCallback?: () => void): void {
      var alreadyOpen = this.state.activePanelName === name;
      this.setState({
        activePanelName: alreadyOpen ? this.state.previousPanelName : name,
        activePanelIsModal: alreadyOpen ? this.state.previousPanelIsModal : !!beModal,
        previousPanelName: this.state.activePanelName,
        previousPanelIsModal: this.state.activePanelIsModal
      }, optionalCallback);
    }

    onRevealedPanel(): void {
      if (this.state.activePanelIsModal) {
        const activeModal = this.getActiveModalElement(this.state.activePanelName);
        if (activeModal) {
          this.focusOnPrimaryOrFirstPossibleElement(activeModal);
        }
      }
    }

    clearActivePanel(optionalCallback?: () => void): void {
      this.setState(this.getDefaultState(), optionalCallback);
    }

    onRenderPanel(panelName: string, panel: Container): void {
      const newPanel: PanelMap = {};
      newPanel[panelName] = panel;
      this.panels = Object.assign({}, this.panels, newPanel);
    }

    handleEscKey(): void {
      if (this.state.activePanelName) {
        this.clearActivePanel();
      }
    }

    onDocumentKeyDown(event: KeyboardEvent): void {
      if (Event.keyPressWasEsc(event)) {
        this.handleEscKey();
      }
    }

    handleModalFocus(event: FocusEvent): void {
      const activeModal = this.state.activePanelIsModal ? this.getActiveModalElement(this.state.activePanelName) : null;
      const focusTarget = event.relatedTarget as Element | null;
      if (!activeModal || !focusTarget) {
        return;
      }
      const possibleMatches = activeModal.getElementsByTagName(focusTarget.tagName);
      const match = Array.prototype.some.call(possibleMatches, function(element: HTMLElement) {
        return element === focusTarget;
      });
      if (!match) {
        event.preventDefault();
        event.stopImmediatePropagation();
        this.focusOnFirstPossibleElement(activeModal);
      }
    }

    getActiveModalElement(panelName: string): Element | null {
      const panel = this.panels[panelName];
      const domNode = panel ? ReactDOM.findDOMNode(panel) : null;
      return domNode && domNode instanceof Element ? domNode : null;
    }

    focusOnPrimaryOrFirstPossibleElement(parentElement: Element): void {
      var primaryElement = parentElement.querySelector<HTMLElement>('button.button-primary');
      if (primaryElement) {
        primaryElement.focus();
      } else {
        this.focusOnFirstPossibleElement(parentElement);
      }
    }

    focusOnFirstPossibleElement(parentElement: Element): void {
      var tabSelector = 'a[href], area[href], input:not([disabled]), button:not([disabled]), select:not([disabled]), textarea:not([disabled]), iframe, object, embed, *[tabindex], *[contenteditable]';
      var firstFocusableElement = parentElement.querySelector<HTMLElement>(tabSelector);
      if (firstFocusableElement) {
        firstFocusableElement.focus();
      }
    }

    componentWillMount(): void {
      if (this.header) {
        this.setState({
          headerHeight: this.header.offsetHeight
        });
      }
    }

    componentDidMount(): void {
      window.document.addEventListener('keydown', this.onDocumentKeyDown, false);
      window.document.addEventListener('focus', this.handleModalFocus, true);
      const container = this.props.feedbackContainer || document.getElementById(Page.feedbackContainerId);
      if (this.footer && container) {
        ReactDOM.render(this.renderFeedbackLink(), container);
      } else if (container && !this.footer) {
        throw new PageFooterRenderingError(this);
      }
    }

    getFooterHeight(): number {
      return this.state.footerHeight;
    }

    getHeaderHeight(): number {
      return this.state.headerHeight;
    }

    resetHeaderHeight(): void {
      if (this.header && this.header.offsetHeight !== this.state.headerHeight) {
        this.setState({
          headerHeight: this.header.offsetHeight
        });
      }
    }

    setFooterHeight(number: number): void {
      this.setState({
        footerHeight: number
      });
    }

    toggleFeedback(): void {
      this.toggleActivePanel('feedback', true);
    }

    onRenderFooter(content?: React.ReactNode, footerClassName?: string) {
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
      this.resetHeaderHeight();
    }

    onRenderNavActions(content: React.ReactNode) {
      // This should use ReactDOM.createPortal when we upgrade to React 16
      const el = this.navActions;
      if (el) {
        ReactDOM.render((
          <div>{content}</div>
        ), el);
      }
      this.resetHeaderHeight();
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
            headerHeight: this.getHeaderHeight(),
            footerHeight: this.getFooterHeight(),
            onRevealedPanel: this.onRevealedPanel,
            ref: (component: React.Component) => this.component = component
          })}
        </div>
      );
    }
}

Page.feedbackContainerId = 'header-feedback';

export default Page;

