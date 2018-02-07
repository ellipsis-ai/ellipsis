/* global BehaviorListConfig:false */
import 'core-js';
import 'whatwg-fetch';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import BehaviorListApp from './app';
import Page from '../shared_ui/page';

ReactDOM.render((
  <Page csrfToken={BehaviorListConfig.csrfToken}>
    <BehaviorListApp {...BehaviorListConfig} />
  </Page>
), document.getElementById(BehaviorListConfig.containerId));
