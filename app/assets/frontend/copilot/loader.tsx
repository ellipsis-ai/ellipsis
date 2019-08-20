import 'core-js';
import 'whatwg-fetch';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import Copilot from './index';
import Page from '../shared_ui/page';

declare var CopilotConfig: {
  containerId: string,
  csrfToken: string,
  channelName: string
};

const container = document.getElementById(CopilotConfig.containerId);

if (container) {
  ReactDOM.render((
    <Page csrfToken={CopilotConfig.csrfToken}
          onRender={(pageProps) => (
            <Copilot {...pageProps} {...CopilotConfig} />
          )}
    />
  ), container);
}
