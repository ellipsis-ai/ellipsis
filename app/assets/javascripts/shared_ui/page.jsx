define(function(require) {
  const React = require('react'),
    ReactDOM = require('react-dom'),
    Event = require('../lib/event'),
    autobind = require('../lib/autobind');

  class Page extends React.Component {
    constructor(props) {
      super(props);
      autobind(this);
      this.state = this.getDefaultState();
    }

    getDefaultState() {
      return {
        activePanelName: "",
        activePanelIsModal: false,
        previousPanelName: "",
        previousPanelIsModal: false
      };
    }

    static requiredPropDefaults() {
      return {
        activePanelName: "",
        activePanelIsModal: false,
        onToggleActivePanel: function() { void(0); },
        onClearActivePanel: function() { void(0); }
      };
    }

    toggleActivePanel(name, beModal, optionalCallback) {
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

    clearActivePanel(optionalCallback) {
      var callback = typeof(optionalCallback) === 'function' ? optionalCallback : null;
      this.setState(this.getDefaultState(), callback);
    }

    handleEscKey() {
      if (this.state.activePanelName) {
        this.clearActivePanel();
      }
    }

    onDocumentKeyDown(event) {
      if (Event.keyPressWasEsc(event)) {
        this.handleEscKey(event);
      }
    }

    handleModalFocus(event) {
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

    getActiveModalElement() {
      if (this.state.activePanelName && this.state.activePanelIsModal) {
        return ReactDOM.findDOMNode(this.component.refs[this.state.activePanelName]);
      } else {
        return null;
      }
    }

    focusOnPrimaryOrFirstPossibleElement(parentElement) {
      var primaryElement = parentElement.querySelector('button.button-primary');
      if (primaryElement) {
        primaryElement.focus();
      } else {
        this.focusOnFirstPossibleElement(parentElement);
      }
    }

    focusOnFirstPossibleElement(parentElement) {
      var tabSelector = 'a[href], area[href], input:not([disabled]), button:not([disabled]), select:not([disabled]), textarea:not([disabled]), iframe, object, embed, *[tabindex], *[contenteditable]';
      var firstFocusableElement = parentElement.querySelector(tabSelector);
      if (firstFocusableElement) {
        firstFocusableElement.focus();
      }
    }

    componentDidMount() {
      window.document.addEventListener('keydown', this.onDocumentKeyDown, false);
      window.document.addEventListener('focus', this.handleModalFocus, true);
    }

    render() {
      return (
        <div className="flex-row-cascade">
          {React.Children.map(this.props.children, (ea) => React.cloneElement(ea, {
            activePanelName: this.state.activePanelName,
            activePanelIsModal: this.state.activePanelIsModal,
            onToggleActivePanel: this.toggleActivePanel,
            onClearActivePanel: this.clearActivePanel,
            ref: (component) => this.component = component
          }))}
        </div>
      );
    }
  }

  Page.propTypes = {
    children: React.PropTypes.node.isRequired
  };

  Page.requiredPropTypes = Object.freeze({
    activePanelName: React.PropTypes.string.isRequired,
    activePanelIsModal: React.PropTypes.bool.isRequired,
    onToggleActivePanel: React.PropTypes.func.isRequired,
    onClearActivePanel: React.PropTypes.func.isRequired
  });

  return Page;
});
