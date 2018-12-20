import 'core-js';
import 'whatwg-fetch';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import ApiTokenGenerator, {ApiTokenGeneratorProps} from './index';
import Page from '../../shared_ui/page';

declare var ApiTokenGeneratorConfig: ApiTokenGeneratorProps & {
  containerId: string
};

ReactDOM.render((
  <Page csrfToken={ApiTokenGeneratorConfig.csrfToken}
    onRender={(pageProps) => (
      <ApiTokenGenerator {...ApiTokenGeneratorConfig} {...pageProps} />
    )}
  />
), document.getElementById(ApiTokenGeneratorConfig.containerId));
