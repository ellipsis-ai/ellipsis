/* global IntegrationEditorConfig:false */
import 'core-js';
import 'whatwg-fetch';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import IntegrationEditor from './index';
import Page from '../../../javascripts/shared_ui/page';

ReactDOM.render((
  <Page csrfToken={IntegrationEditorConfig.csrfToken}>
    <IntegrationEditor {...IntegrationEditorConfig} />
  </Page>
), document.getElementById(IntegrationEditorConfig.containerId));
