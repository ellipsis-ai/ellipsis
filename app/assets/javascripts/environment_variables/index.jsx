define(function(require) {
  var React = require('react'),
    SettingsMenu = require('../shared_ui/settings_menu'),
    Setter = require('./setter'),
    ifPresent = require('../lib/if_present'),
    Sort = require('../lib/sort');

  return React.createClass({
    displayName: 'EnvironmentVariableList',
    propTypes: {
      csrfToken: React.PropTypes.string.isRequired,
      data: React.PropTypes.shape({
        teamId: React.PropTypes.string.isRequired,
        variables: React.PropTypes.arrayOf(React.PropTypes.shape({
          name: React.PropTypes.string.isRequired,
          isAlreadySavedWithValue: React.PropTypes.bool.isRequired
        })).isRequired
      })
    },

    getInitialState: function() {
      return {
        activePanel: null,
        environmentVariables: this.groupAndSortVarsByNameAndPresenceOfValue(this.props.data.variables),
        justSaved: false
      };
    },

    groupAndSortVarsByNameAndPresenceOfValue: function(vars) {
      return Sort.arrayAlphabeticalBy(vars, (ea) => {
        // Group vars with existing values before those without
        return ea.isAlreadySavedWithValue ? `-${ea.name}` : `~${ea.name}`;
      });
    },

    onSave: function(envVars) {
      this.setState({
        justSaved: false
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
            justSaved: true
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

    render: function() {
      return (
        <div className="flex-row-cascade">
          <div className="bg-light">
            <div className="container container-wide pbm">
              {this.renderHeader()}
            </div>
          </div>
          <div className="flex-columns flex-row-expand">
            <div className="flex-column flex-column-left flex-rows container container-wide prn">
              <div className="columns flex-columns flex-row-expand">
                <div className="column column-one-quarter flex-column">
                  <SettingsMenu activePage="environmentVariables" teamId={this.props.data.teamId} />
                </div>
                <div className="column column-three-quarters flex-column bg-white ptxl pbxxxxl phxxxxl">

                  {this.renderEnvVarList()}

                </div>
              </div>
            </div>
            <div className="flex-column flex-column-right bg-white" />
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
          onSave={this.onSave}
          vars={this.getVars()}
          isFullPage={true}
        />
      );
    }

  });
});
