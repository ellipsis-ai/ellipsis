import * as React from 'react';
import SettingsPage from '../../shared_ui/settings_page';
import Setter from './setter';
import Sort from '../../lib/sort';
import {PageRequiredProps} from '../../shared_ui/page';
import {EnvironmentVariableData, EnvironmentVariableListConfig} from "./loader";
import autobind from "../../lib/autobind";
import {DataRequest} from "../../lib/data_request";

type Props = EnvironmentVariableListConfig & PageRequiredProps

interface State {
  environmentVariables: Array<EnvironmentVariableData>
}

class EnvironmentVariableList extends React.Component<Props, State> {
  setterComponent: Option<Setter>;

  constructor(props: Props) {
    super(props);
    autobind(this);
    this.setterComponent = null;
    this.state = {
      environmentVariables: this.groupAndSortVarsByNameAndPresenceOfValue(this.props.data.variables)
    };
  }

  groupAndSortVarsByNameAndPresenceOfValue(vars: Array<EnvironmentVariableData>): Array<EnvironmentVariableData> {
    return Sort.arrayAlphabeticalBy(vars, (ea) => {
      // Group vars with existing values before those without
      return ea.isAlreadySavedWithValue ? `-${ea.name}` : `~${ea.name}`;
    });
  }

  onSave(envVars: Array<EnvironmentVariableData>) {
    var url = jsRoutes.controllers.web.settings.EnvironmentVariablesController.submit().url;
    var data = {
      teamId: this.props.data.teamId,
      variables: envVars
    };
    DataRequest.jsonPost(url, {
      teamId: this.props.data.teamId,
      dataJson: JSON.stringify(data)
    }, this.props.csrfToken).then((json) => {
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
  }

  onDelete(name: string) {
    this.setState({
      environmentVariables: this.state.environmentVariables.filter((ea) => ea.name !== name)
    });
  }

  loadAdminValue(name: string, value: string): void {
    this.setState({
      environmentVariables: this.state.environmentVariables.map((ea) => {
        if (ea.name === name) {
          return {
            name: name,
            value: value,
            isAlreadySavedWithValue: Boolean(value)
          };
        } else {
          return ea;
        }
      })
    });
  }

  getVars(): Array<EnvironmentVariableData> {
    return this.state.environmentVariables;
  }

  render() {
    return (
      <SettingsPage teamId={this.props.data.teamId} isAdmin={this.props.isAdmin} activePage={"environmentVariables"}>
        {this.renderEnvVarList()}
      </SettingsPage>
    );
  }

  renderEnvVarList() {
    return (
      <Setter
        ref={(el) => this.setterComponent = el}
        onSave={this.onSave}
        onDelete={this.onDelete}
        vars={this.getVars()}
        focus={this.props.focus}
        onRenderFooter={this.props.onRenderFooter}
        activePanelName={this.props.activePanelName}
        activePanelIsModal={this.props.activePanelIsModal}
        onToggleActivePanel={this.props.onToggleActivePanel}
        onClearActivePanel={this.props.onClearActivePanel}
        teamId={this.props.data.teamId}
        csrfToken={this.props.csrfToken}
        isAdmin={this.props.isAdmin}
        onAdminLoadedValue={this.loadAdminValue}
      />
    );
  }
}

export default EnvironmentVariableList;
