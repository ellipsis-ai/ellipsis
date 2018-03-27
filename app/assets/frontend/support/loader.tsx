import 'core-js';
import 'whatwg-fetch';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import Page from '../shared_ui/page';
import SupportRequest, {SupportRequestProps} from "./index";

declare var SupportRequestConfig: {
  containerId: string
} & SupportRequestProps;

const container = document.getElementById(SupportRequestConfig.containerId);

if (container) {
  ReactDOM.render((
    <Page csrfToken={SupportRequestConfig.csrfToken}
      onRender={(pageProps) => (
        <SupportRequest {...SupportRequestConfig} {...pageProps} />
      )}
    />
  ), container);
}
