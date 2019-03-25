import 'core-js';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import IntegrationList, {IntegrationListProps} from './index';
import Page from '../../shared_ui/page';

declare var IntegrationListConfig: IntegrationListProps & {
  containerId: string
};

ReactDOM.render((
  <Page
    csrfToken={IntegrationListConfig.csrfToken}
    onRender={(pageProps) => (
      <IntegrationList {...IntegrationListConfig} {...pageProps} />
    )}
  />
), document.getElementById(IntegrationListConfig.containerId));
