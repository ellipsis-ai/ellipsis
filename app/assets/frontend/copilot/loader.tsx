import 'core-js';
import 'whatwg-fetch';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import Copilot from './index';
import Page from '../shared_ui/page';

type Config = {
  containerId: string,
  csrfToken: string
}

declare var CopilotConfig: Config;

const container = document.getElementById(CopilotConfig.containerId);

if (container) {
  ReactDOM.render((
    <Page csrfToken={CopilotConfig.csrfToken}
          onRender={(pageProps) => (
            <Copilot {...pageProps} csrfToken={CopilotConfig.csrfToken} />
          )}
    />
  ), container);
}
