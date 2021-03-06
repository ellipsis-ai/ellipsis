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
  onRenderHeader: (content?: React.ReactNode) => any,
  onRenderFooter: (content?: React.ReactNode, footerClassName?: Option<string>, footerStyle?: Option<React.CSSProperties>) => any,
  onRenderNavItems: (items: Array<NavItemContent>) => void,
  onRenderNavActions: (content: React.ReactNode) => void,
  onRenderPanel: (panelName: string, panel: Container) => void,
  onRevealedPanel: () => void,
  headerHeight: number,
  footerHeight: number,
  ref?: any
  isMobile: boolean
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
import * as debounce from "javascript-debounce";
import {MOBILE_MAX_WIDTH} from "../lib/constants";

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
  isMobile: boolean
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
    debounceOnResize: () => void;
    setFooterHeight: (newHeight: number) => void;

    constructor(props: Props) {
      super(props);
      autobind(this);
      this.state = this.getDefaultState();
      this.footer = null;
      this.panels = {};
      this.navItems = document.getElementById("mainNavItems");
      this.navActions = document.getElementById("mainNavActions");
      this.header = document.getElementById("main-header");
      this.debounceOnResize = debounce(this.onResize, 150);
      this.setFooterHeight = debounce(this._setFooterHeight, 25);
    }

    getDefaultState(): State {
      return {
        activePanelName: "",
        activePanelIsModal: false,
        previousPanelName: "",
        previousPanelIsModal: false,
        headerHeight: this.state ? this.state.headerHeight : 0,
        footerHeight: this.state ? this.state.footerHeight : 0,
        isMobile: window.innerWidth <= MOBILE_MAX_WIDTH
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
      const primaryElement = parentElement.querySelector<HTMLElement>('button.button-primary:not([disabled])');
      if (primaryElement) {
        // Avoid a problem where two focus events occur nearly simultaneously by grabbing focus asynchronously
        setTimeout(() => {
          primaryElement.focus();
        }, 25);
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
      window.addEventListener('resize', this.debounceOnResize);
      const feedbackContainer = this.props.feedbackContainer || document.getElementById(Page.feedbackContainerId);
      if (feedbackContainer && !this.footer) {
        throw new PageFooterRenderingError(this);
      }
    }

    componentDidUpdate(): void {
      this.onResize();
    }

    getFooterHeight(): number {
      return this.state.footerHeight;
    }

    getHeaderHeight(): number {
      return this.state.headerHeight;
    }

    onResize(): void {
      if (this.header && this.header.offsetHeight !== this.state.headerHeight) {
        this.setState({
          headerHeight: this.header.offsetHeight
        });
      }
      if (this.state.isMobile !== (window.innerWidth <= MOBILE_MAX_WIDTH)) {
        this.setState({
          isMobile: window.innerWidth <= MOBILE_MAX_WIDTH
        });
      }
    }

    _setFooterHeight(number: number): void {
      this.setState({
        footerHeight: number
      });
    }

    toggleFeedback(): void {
      this.toggleActivePanel('feedback', true);
    }

    onRenderFooter(content?: React.ReactNode, footerClassName?: Option<string>, footerStyle?: Option<React.CSSProperties>) {
      return (
        <div>
          <ModalScrim isActive={this.state.activePanelIsModal} onClick={this.clearActivePanel} />
          <FixedFooter ref={(el) => this.footer = el}
            className={`bg-white ${footerClassName || ""}`}
            style={footerStyle}
            onHeightChange={this.setFooterHeight}
          >
            <Collapsible revealWhen={this.state.activePanelName === 'feedback'}>
              <FeedbackPanel onDone={this.toggleFeedback} csrfToken={this.props.csrfToken} />
            </Collapsible>
            {content}
          </FixedFooter>
        </div>
      );
    }

    onRenderHeader(content?: React.ReactNode) {
      if (this.header) {
        return ReactDOM.createPortal(content, this.header);
      } else {
        return null;
      }
    }

    onRenderNavItems(navItems: Array<NavItemContent>) {
      if (this.navItems) {
        return ReactDOM.createPortal(
          navItems.map((ea, index) => (
            <NavItem key={`navItem${index}`} title={ea.title} url={ea.url} callback={ea.callback} />
          )), this.navItems);
      } else {
        return null;
      }
    }

    onRenderNavActions(content: React.ReactNode) {
      const el = this.navActions;
      if (el) {
        return ReactDOM.createPortal(content, el);
      } else {
        return null;
      }
    }

    renderFeedbackLink() {
      const container = this.props.feedbackContainer || document.getElementById(Page.feedbackContainerId);
      if (container) {
        return ReactDOM.createPortal((
          <Button className="button-dropdown-item plm" onClick={this.toggleFeedback}>Feedback</Button>
        ), container);
      } else {
        return null;
      }
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
            onRenderHeader: this.onRenderHeader,
            onRenderPanel: this.onRenderPanel,
            onRenderNavItems: this.onRenderNavItems,
            onRenderNavActions: this.onRenderNavActions,
            headerHeight: this.getHeaderHeight(),
            footerHeight: this.getFooterHeight(),
            onRevealedPanel: this.onRevealedPanel,
            isMobile: this.state.isMobile,
            ref: (component: React.Component) => this.component = component
          })}
          {this.renderFeedbackLink()}
        </div>
      );
    }
}

Page.feedbackContainerId = 'header-feedback';

export default Page;

