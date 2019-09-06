import 'core-js';
import 'whatwg-fetch';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import Copilot, {Listener} from './index';
import Page from '../shared_ui/page';
import User, {UserJson} from "../models/user";

declare var CopilotConfig: {
  containerId: string,
  csrfToken: string,
  teamName: string,
  listener: Listener
  user: UserJson
};

const container = document.getElementById(CopilotConfig.containerId);

if (container) {
  ReactDOM.render((
    <Page csrfToken={CopilotConfig.csrfToken}
          onRender={(pageProps) => (
            <Copilot
              {...pageProps}
              csrfToken={CopilotConfig.csrfToken}
              teamName={CopilotConfig.teamName}
              listener={CopilotConfig.listener}
              user={User.fromJson(CopilotConfig.user)}
            />
          )}
    />
  ), container);
}
