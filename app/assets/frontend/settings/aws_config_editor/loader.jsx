/* global AwsConfigEditorConfig:false */
import 'core-js';
import 'whatwg-fetch';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import AwsConfigEditor from './index';
import Page from '../../shared_ui/page';

ReactDOM.render((
  <Page csrfToken={AwsConfigEditorConfig.csrfToken}>
    <AwsConfigEditor {...AwsConfigEditorConfig} />
  </Page>
), document.getElementById(AwsConfigEditorConfig.containerId));
