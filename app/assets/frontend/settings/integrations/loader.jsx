/* global IntegrationListConfig:false */
import 'core-js';
import 'whatwg-fetch';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import IntegrationList from './index';
import Page from '../../../javascripts/shared_ui/page';

ReactDOM.render((
  <Page csrfToken={IntegrationListConfig.csrfToken}>
    <IntegrationList {...IntegrationListConfig} />
  </Page>
), document.getElementById(IntegrationListConfig.containerId));
