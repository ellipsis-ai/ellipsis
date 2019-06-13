import 'core-js';
import 'whatwg-fetch';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import Page from '../shared_ui/page';
import autobind from "../lib/autobind";
import SchedulingDataLayer, {SchedulingConfigInterface} from "./data_layer";

declare var SchedulingConfig: SchedulingConfigInterface;

class SchedulingLoader extends React.Component<SchedulingConfigInterface> {
  constructor(props: SchedulingConfigInterface) {
    super(props);
    autobind(this);
  }

  render() {
    return (
      <Page onRender={(pageProps) => (
        <SchedulingDataLayer {...this.props} pageProps={pageProps} sidebarWidth={0} />
      )} csrfToken={this.props.csrfToken} />
    )
  }
}

if (typeof SchedulingConfig !== "undefined") {
  const container = document.getElementById(SchedulingConfig.containerId);
  if (container) {
    ReactDOM.render((
      <SchedulingLoader {...SchedulingConfig} />
    ), container);
  }
}
