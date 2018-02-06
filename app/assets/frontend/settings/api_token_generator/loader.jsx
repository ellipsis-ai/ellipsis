/* global ApiTokenGeneratorConfig:false */
import 'core-js';
import 'whatwg-fetch';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import ApiTokenGenerator from './index';
import Page from '../../shared_ui/page';

ReactDOM.render((
  <Page csrfToken={ApiTokenGeneratorConfig.csrfToken}>
    <ApiTokenGenerator {...ApiTokenGeneratorConfig} />
  </Page>
), document.getElementById(ApiTokenGeneratorConfig.containerId));
