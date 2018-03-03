/* global AwsConfigEditorConfig:false */
import 'core-js';
import 'whatwg-fetch';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import AwsConfigEditor from './index';
import Page from '../../shared_ui/page';

ReactDOM.render((
  <Page csrfToken={AwsConfigEditorConfig.csrfToken}
    onRender={(pageProps) => (
      <AwsConfigEditor {...AwsConfigEditorConfig} {...pageProps} />
    )}
  />
), document.getElementById(AwsConfigEditorConfig.containerId));
