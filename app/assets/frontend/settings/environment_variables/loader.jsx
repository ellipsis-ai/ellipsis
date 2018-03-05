/* global EnvironmentVariableListConfig:false */
import 'core-js';
import 'whatwg-fetch';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import EnvironmentVariableList from './index';
import Page from '../../shared_ui/page';

ReactDOM.render((
  <Page csrfToken={EnvironmentVariableListConfig.csrfToken}
    onRender={(pageProps) => (
      <EnvironmentVariableList {...EnvironmentVariableListConfig} {...pageProps} />
    )}
  />
), document.getElementById(EnvironmentVariableListConfig.containerId));
