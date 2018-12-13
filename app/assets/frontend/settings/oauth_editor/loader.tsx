import 'core-js';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import IntegrationEditor, {OAuthEditorProps} from './index';
import Page from '../../shared_ui/page';

declare var IntegrationEditorConfig: OAuthEditorProps & {
  containerId: string
};

ReactDOM.render((
  <Page csrfToken={IntegrationEditorConfig.csrfToken}
    onRender={(pageProps) => (
      <IntegrationEditor {...IntegrationEditorConfig} {...pageProps} />
    )}
  />
), document.getElementById(IntegrationEditorConfig.containerId));
