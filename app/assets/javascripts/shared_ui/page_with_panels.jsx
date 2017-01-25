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
            activePanel: null
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
          var alreadyOpen = this.getActivePanelName() === name;
          this.setState({
            activePanel: alreadyOpen ? null : { name: name, isModal: !!beModal }
          }, optionalCallback);
        },

        clearActivePanel: function() {
          this.setState({
            activePanel: null
          });
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
