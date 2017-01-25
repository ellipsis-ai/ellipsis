define(function(require) {
  var React = require('react'),
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
          this.setState({
            activePanelName: alreadyOpen ? this.state.previousPanelName : name,
            activePanelIsModal: alreadyOpen ? this.state.previousPanelIsModal : !!beModal,
            previousPanelName: this.state.activePanelName,
            previousPanelIsModal: this.state.activePanelIsModal
          }, optionalCallback);
        },

        clearActivePanel: function() {
          this.setState(this.getInitialState());
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

        componentDidMount: function() {
          window.document.addEventListener('keydown', this.onDocumentKeyDown, false);
        },

        render: function() {
          return (
            <Component
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
