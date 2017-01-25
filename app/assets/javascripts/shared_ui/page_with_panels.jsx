define(function(require) {
  var React = require('react');

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
            activePanel: null,
            previousActivePanel: null
          };
        },

        getActivePanelName: function() {
          return this.state.activePanel && this.state.activePanel.name ? this.state.activePanel.name : "";
        },

        activePanelIsModal: function() {
          const panel = this.state.activePanel;
          return !!(panel && panel.isModal);
        },

        toggleActivePanel: function(name, beModal, optionalCallback) {
          var previousPanel = this.state.previousActivePanel;
          var alreadyOpen = this.getActivePanelName() === name;
          var newPanel = alreadyOpen ? previousPanel : { name: name, isModal: !!beModal };
          this.setState({
            activePanel: newPanel,
            previousActivePanel: this.state.activePanel
          }, optionalCallback);
        },

        clearActivePanel: function() {
          this.setState(this.getInitialState());
        },

        render: function() {
          return (
            <Component
              activePanelName={this.getActivePanelName()}
              activePanelIsModal={this.activePanelIsModal()}
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
