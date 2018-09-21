import 'core-js';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import EnvironmentVariableList from './index';
import Page from '../../shared_ui/page';

export interface EnvironmentVariableData {
  name: string,
  isAlreadySavedWithValue: boolean,
  value?: Option<string>
}

export interface EnvironmentVariablesData {
  teamId: string,
  variables: Array<EnvironmentVariableData>,
  error?: Option<string>
}

export interface EnvironmentVariableListConfig {
  containerId: string,
  csrfToken: string,
  isAdmin: boolean,
  data: EnvironmentVariablesData,
  focus?: Option<string>
}

declare var EnvironmentVariableListConfig: EnvironmentVariableListConfig;

const container = document.getElementById(EnvironmentVariableListConfig.containerId);
if (container) {
  ReactDOM.render((
    <Page csrfToken={EnvironmentVariableListConfig.csrfToken}
      onRender={(pageProps) => (
        <EnvironmentVariableList {...EnvironmentVariableListConfig} {...pageProps} />
      )}
    />
  ), container);
}
