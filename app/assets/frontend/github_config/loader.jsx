// @flow

import 'core-js';
import 'whatwg-fetch';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import GithubConfig from './index';
import type {GithubConfigProps} from './index';
// TODO: remove "any" hack once Page is converted to ES6 module
import PageComponent from '../../javascripts/shared_ui/page';
const Page: any = PageComponent;

type Config = {
  containerId: string
}

declare var GithubConfigConfig: Config & GithubConfigProps;

const container = document.getElementById(GithubConfigConfig.containerId);

if (container) {
  ReactDOM.render((
    <Page csrfToken={GithubConfigConfig.csrfToken}>
      <GithubConfig {...GithubConfigConfig} />
    </Page>
  ), container);
}
