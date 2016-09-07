define(function(require) {
  var React = require('react'),
    SettingsMenu = require('../settings_menu'),
    Setter = require('./setter'),
    ifPresent = require('../if_present');
    require('whatwg-fetch');

  return React.createClass({
    displayName: 'EnvironmentVariableList',
    propTypes: {
      csrfToken: React.PropTypes.string.isRequired,
      data: React.PropTypes.shape({
        teamId: React.PropTypes.string.isRequired,
        variables: React.PropTypes.arrayOf(React.PropTypes.shape({
          name: React.PropTypes.string.isRequired,
          isAlreadySavedWithName: React.PropTypes.bool.isRequired,
          isAlreadySavedWithValue: React.PropTypes.bool.isRequired
        })).isRequired
      })
    },

    getInitialState: function() {
      return {
        activePanel: null,
        environmentVariables: this.props.data.variables,
        justSaved: false,
        saving: false
      };
    },

    onSave: function(envVars) {
      this.setState({
        justSaved: false,
        saving: true
      }, () => { this.save(envVars); });
    },

    save: function(envVars) {
      var url = jsRoutes.controllers.EnvironmentVariablesController.submit().url;
      var data = {
        teamId: this.props.data.teamId,
        variables: envVars
      };
      fetch(url, {
        credentials: 'same-origin',
        method: 'POST',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
          'Csrf-Token': this.props.csrfToken
        },
        body: JSON.stringify({ teamId: this.props.data.teamId, dataJson: JSON.stringify(data) })
      })
        .then((response) => response.json())
        .then((json) => {
          this.setState({
            environmentVariables: json.variables,
            justSaved: true,
            saving: false
          }, () => {
            this.refs.setter.reset();
          });
        }).catch(() => {
          this.refs.envVariableSetterPanel.onSaveError();
        });
    },

    getVars: function() {
      return this.state.environmentVariables;
    },

    getSaveButtonLabel: function() {
      return this.state.saving ? "Saving…" : "Save";
    },

    render: function() {
      return (
        <div>
          <div className="bg-light">
            <div className="container pbm">
              {this.renderHeader()}
            </div>
          </div>
          <div className="flex-container">
            <div className="container flex flex-center">
              <div className="columns">
                <div className="column column-one-quarter">
                  <SettingsMenu activePage="environmentVariables"/>
                </div>
                <div className="column column-three-quarters bg-white border-radius-bottom-left ptxl pbxxxxl phxxxxl">

                  {this.renderEnvVarList()}

                </div>
              </div>
            </div>
            <div className="flex flex-left"></div>
            <div className="flex flex-right bg-white"></div>
          </div>
        </div>
      );
    },

    renderHeader: function() {
      return (
        <h3 className="mvn ptxxl type-weak display-ellipsis">
          <span className="mrs">Environment variables</span>
          {ifPresent(this.state.justSaved, () => (<span className="type-green fade-in"> — saved successfully</span>))}
        </h3>
      );
    },

    renderEnvVarList: function() {
      return (
        <Setter
          ref="setter"
          onChangeVarName={function(){}}
          onSave={this.onSave}
          vars={this.getVars()}
          saveButtonLabel={this.getSaveButtonLabel()}
        />
      );
    }

  });
});
