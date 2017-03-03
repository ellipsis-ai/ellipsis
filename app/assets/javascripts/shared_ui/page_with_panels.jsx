define(function(require) {
  var React = require('react'),
    ReactDOM = require('react-dom'),
    Event = require('../lib/event');

  return {
    requiredPropTypes: function() {
      return {
        activePanelName: React.PropTypes.string.isRequired,
        activePanelIsModal: React.PropTypes.bool.isRequired,
        onToggleActivePanel: React.PropTypes.func.isRequired,
        onClearActivePanel: React.PropTypes.func.isRequired
      };
    },

    with: function(Component) {
      return React.createClass({
        displayName: 'PageWithPanels',

        getInitialState: function() {
          return {
            activePanelName: "",
            activePanelIsModal: false,
            previousPanelName: "",
            previousPanelIsModal: false
          };
        },

        toggleActivePanel: function(name, beModal, optionalCallback) {
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
        },

        clearActivePanel: function(optionalCallback) {
          var callback = typeof(optionalCallback) === 'function' ? optionalCallback : null;
          this.setState(this.getInitialState(), callback);
        },

        handleEscKey: function() {
          if (this.state.activePanelName) {
            this.clearActivePanel();
          }
        },

        onDocumentKeyDown: function(event) {
          if (Event.keyPressWasEsc(event)) {
            this.handleEscKey(event);
          }
        },

        handleModalFocus: function(event) {
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
        },

        getActiveModalElement: function() {
          if (this.state.activePanelName && this.state.activePanelIsModal) {
            return ReactDOM.findDOMNode(this.refs.component.refs[this.state.activePanelName]);
          } else {
            return null;
          }
        },

        focusOnPrimaryOrFirstPossibleElement: function(parentElement) {
          var primaryElement = parentElement.querySelector('button.button-primary');
          if (primaryElement) {
            primaryElement.focus();
          } else {
            this.focusOnFirstPossibleElement(parentElement);
          }
        },

        focusOnFirstPossibleElement: function(parentElement) {
          var tabSelector = 'a[href], area[href], input:not([disabled]), button:not([disabled]), select:not([disabled]), textarea:not([disabled]), iframe, object, embed, *[tabindex], *[contenteditable]';
          var firstFocusableElement = parentElement.querySelector(tabSelector);
          if (firstFocusableElement) {
            firstFocusableElement.focus();
          }
        },

        componentDidMount: function() {
          window.document.addEventListener('keydown', this.onDocumentKeyDown, false);
          window.document.addEventListener('focus', this.handleModalFocus, true);
        },

        render: function() {
          return (
            <Component
              ref="component"
              activePanelName={this.state.activePanelName}
              activePanelIsModal={this.state.activePanelIsModal}
              onToggleActivePanel={this.toggleActivePanel}
              onClearActivePanel={this.clearActivePanel}
              {...this.props} {...this.state}
            />
          );
        }
      });
    }
  };
});
