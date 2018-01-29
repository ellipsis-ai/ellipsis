// @flow
import type {Node, ElementType} from 'react';
export type PageRequiredProps = {
  activePanelName: string,
  activePanelIsModal: boolean,
  onToggleActivePanel: (name: string, beModal?: boolean, optionalCallback?: () => void) => void,
  onClearActivePanel: (optionalCallback?: () => void) => void,
  onRenderFooter: (content: Node, footerClassName?: string) => Node,
  footerHeight: number,
  onRenderPanel: (panelName: string, panel: ElementType) => void
};

define(function(require) {
  const React = require('react'),
    ReactDOM = require('react-dom'),
    Event = require('../lib/event'),
    FeedbackPanel = require('../panels/feedback'),
    Collapsible = require('./collapsible'),
    FixedFooter = require('./fixed_footer'),
    ModalScrim = require('./modal_scrim'),
    Button = require('../form/button'),
    autobind = require('../lib/autobind'),
    PageFooterRenderingError = require('./page_footer_rendering_error');

  type Props = {
    children: React.Node,
    csrfToken: string,
    feedbackContainer: ?{}
  }

  type State = {
    activePanelName: string,
    activePanelIsModal: boolean,
    previousPanelName: string,
    previousPanelIsModal: boolean,
    footerHeight: number
  }

  class Page extends React.Component<Props, State> {
    props: Props;
    state: State;
    panels: { [string]: ?ElementType };

    constructor(props: Props) {
      super(props);
      autobind(this);
      this.state = this.getDefaultState();
      this.footer = null;
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
          var activeModal = this.getActiveModalElement();
          if (activeModal) {
            this.focusOnPrimaryOrFirstPossibleElement(activeModal);
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
      var callback = typeof(optionalCallback) === 'function' ? optionalCallback : null;
      this.setState(this.getDefaultState(), callback);
    }

    onRenderPanel(panelName: string, panel: ElementType): void {
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
      var activeModal = this.getActiveModalElement();
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

    getActiveModalElement(): any {
      const panel = this.state.activePanelName && this.state.activePanelIsModal ?
        this.panels[this.state.activePanelName] : null;
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
      if (this.footer) {
        ReactDOM.render(
          this.renderFeedbackLink(),
          this.props.feedbackContainer || document.getElementById(Page.feedbackContainerId)
        );
      } else {
        throw new PageFooterRenderingError(this);
      }
    }

    getFooterHeight(): number {
      return this.state.footerHeight;
    }

    setFooterHeight(number): void {
      this.setState({
        footerHeight: number
      });
    }

    toggleFeedback(): void {
      this.toggleActivePanel('feedback', true);
    }

    onRenderFooter(content?: React.Node, footerClassName?: string) {
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

    renderFeedbackLink(): React.Node {
      return (
        <Button className="button-nav mhs pbm mobile-pbs" onClick={this.toggleFeedback}>Feedback</Button>
      );
    }

    render(): React.Node {
      return (
        <div className="flex-row-cascade">
          {React.Children.map(this.props.children, (ea) => React.cloneElement(ea, {
            activePanelName: this.state.activePanelName,
            activePanelIsModal: this.state.activePanelIsModal,
            onToggleActivePanel: this.toggleActivePanel,
            onClearActivePanel: this.clearActivePanel,
            onRenderFooter: this.onRenderFooter,
            onRenderPanel: this.onRenderPanel,
            footerHeight: this.getFooterHeight(),
            ref: (component) => this.component = component
          }))}
        </div>
      );
    }

    static requiredPropDefaults() {
      return {
        activePanelName: "",
        activePanelIsModal: false,
        onToggleActivePanel: Page.placeholderCallback,
        onClearActivePanel: Page.placeholderCallback,
        onRenderFooter: Page.placeholderCallback,
        footerHeight: 0
      };
    }

    static placeholderCallback() {
      void(0);
    }
  }

  Page.propTypes = {
    children: React.PropTypes.node.isRequired,
    csrfToken: React.PropTypes.string.isRequired,
    feedbackContainer: React.PropTypes.object
  };

  Page.requiredPropTypes = Object.freeze({
    activePanelName: React.PropTypes.string.isRequired,
    activePanelIsModal: React.PropTypes.bool.isRequired,
    onToggleActivePanel: React.PropTypes.func.isRequired,
    onClearActivePanel: React.PropTypes.func.isRequired,
    onRenderFooter: React.PropTypes.func.isRequired,
    footerHeight: React.PropTypes.number.isRequired
  });

  Page.feedbackContainerId = 'header-feedback';

  return Page;
});
