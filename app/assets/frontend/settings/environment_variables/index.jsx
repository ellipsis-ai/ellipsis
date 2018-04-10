import React from 'react';
import SettingsPage from '../../shared_ui/settings_page';
import Setter from './setter';
import Sort from '../../lib/sort';
import Page from '../../shared_ui/page';

const EnvironmentVariableList = React.createClass({
    propTypes: Object.assign({}, Page.requiredPropTypes, {
      csrfToken: React.PropTypes.string.isRequired,
      isAdmin: React.PropTypes.bool.isRequired,
      data: React.PropTypes.shape({
        teamId: React.PropTypes.string.isRequired,
        variables: React.PropTypes.arrayOf(React.PropTypes.shape({
          name: React.PropTypes.string.isRequired,
          isAlreadySavedWithValue: React.PropTypes.bool.isRequired
        })).isRequired
      }),
      focus: React.PropTypes.string
    }),

    setterComponent: null,

    getDefaultProps: function() {
      return Page.requiredPropDefaults();
    },

    getInitialState: function() {
      return {
        activePanel: null,
        environmentVariables: this.groupAndSortVarsByNameAndPresenceOfValue(this.props.data.variables)
      };
    },

    groupAndSortVarsByNameAndPresenceOfValue: function(vars) {
      return Sort.arrayAlphabeticalBy(vars, (ea) => {
        // Group vars with existing values before those without
        return ea.isAlreadySavedWithValue ? `-${ea.name}` : `~${ea.name}`;
      });
    },

    onSave: function(envVars) {
      var url = jsRoutes.controllers.web.settings.EnvironmentVariablesController.submit().url;
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
            environmentVariables: json.variables
          }, () => {
            if (this.setterComponent) {
              this.setterComponent.onSaveComplete();
            }
          });
        }).catch(() => {
          if (this.setterComponent) {
            this.setterComponent.onSaveError();
          }
        });
    },

    getVars: function() {
      return this.state.environmentVariables;
    },

    render: function() {
      return (
        <SettingsPage teamId={this.props.data.teamId} isAdmin={this.props.isAdmin} activePage={"environmentVariables"}>
          {this.renderEnvVarList()}
        </SettingsPage>
      );
    },

    renderEnvVarList: function() {
      return (
        <Setter
          ref={(el) => this.setterComponent = el}
          onSave={this.onSave}
          vars={this.getVars()}
          focus={this.props.focus}
          onRenderFooter={this.props.onRenderFooter}
          activePanelIsModal={this.props.activePanelIsModal}
        />
      );
    }

  });

export default EnvironmentVariableList;

