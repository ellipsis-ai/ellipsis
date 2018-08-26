/* global IntegrationEditorConfig:false */
import 'core-js';
import 'whatwg-fetch';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import IntegrationEditor from './index';
import Page from '../../shared_ui/page';

ReactDOM.render((
  <Page csrfToken={IntegrationEditorConfig.csrfToken}
    onRender={(pageProps) => (
      <IntegrationEditor {...IntegrationEditorConfig} {...pageProps} />
    )}
  />
), document.getElementById(IntegrationEditorConfig.containerId));
