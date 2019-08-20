import 'core-js';
import 'whatwg-fetch';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import Copilot, {Listener} from './index';
import Page from '../shared_ui/page';

declare var CopilotConfig: {
  containerId: string,
  csrfToken: string,
  listener: Listener
};

const container = document.getElementById(CopilotConfig.containerId);

if (container) {
  ReactDOM.render((
    <Page csrfToken={CopilotConfig.csrfToken}
          onRender={(pageProps) => (
            <Copilot {...pageProps}
              csrfToken={CopilotConfig.csrfToken}
              listener={CopilotConfig.listener}
            />
          )}
    />
  ), container);
}
