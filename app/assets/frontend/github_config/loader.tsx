import 'core-js';
import 'whatwg-fetch';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import GithubConfig from './index';
import {GithubConfigProps} from './index';
import Page from '../shared_ui/page';

type Config = {
  containerId: string
}

declare var GithubConfigConfig: Config & GithubConfigProps;

const container = document.getElementById(GithubConfigConfig.containerId);

if (container) {
  ReactDOM.render((
    <Page csrfToken={GithubConfigConfig.csrfToken}
      onRender={(pageProps) => (
        <GithubConfig {...GithubConfigConfig} {...pageProps} />
      )}
    />
  ), container);
}
