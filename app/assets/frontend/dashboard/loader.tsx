import 'core-js';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import autobind from '../lib/autobind';
import Page from "../shared_ui/page";
import Dashboard from "./index";

interface Props {
  containerId: string
  csrfToken: string
}

declare var DashboardConfig: Props;

class DashboardLoader extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  render() {
    return (
      <Page csrfToken={this.props.csrfToken} onRender={(pageProps) => (
        <Dashboard csrfToken={this.props.csrfToken} {...pageProps} />
      )} />
    );
  }
}

const container = document.getElementById(DashboardConfig.containerId);
if (container) {
  ReactDOM.render((
    <DashboardLoader {...DashboardConfig} />
  ), container);
}
