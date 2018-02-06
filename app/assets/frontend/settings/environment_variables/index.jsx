import React from 'react';
import SettingsPage from '../../../javascripts/shared_ui/settings_page';
import Setter from './setter';
import ifPresent from '../../../javascripts/lib/if_present';
import Sort from '../../../javascripts/lib/sort';
import Page from '../../../javascripts/shared_ui/page';

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
            environmentVariables: json.variables,
            justSaved: true
          }, () => {
            if (this.setterComponent) {
              this.setterComponent.reset();
            }
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
        <SettingsPage teamId={this.props.data.teamId} isAdmin={this.props.isAdmin} header={this.renderHeader()} activePage={"environmentVariables"}>
          {this.renderEnvVarList()}
          {this.props.onRenderFooter()}
        </SettingsPage>
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

