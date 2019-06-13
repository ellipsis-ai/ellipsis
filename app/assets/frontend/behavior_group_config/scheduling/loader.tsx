import 'core-js';
import 'whatwg-fetch';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import Page from '../../shared_ui/page';
import autobind from "../../lib/autobind";
import {SchedulingConfigInterface} from "../../scheduling/loader";

declare var BehaviorGroupSchedulingConfig: SchedulingConfigInterface;

class BehaviorGroupSchedulingLoader extends React.Component<SchedulingConfigInterface> {
  constructor(props: SchedulingConfigInterface) {
    super(props);
    autobind(this);
  }

  render() {
    return (
      <Page csrfToken={this.props.csrfToken}
        onRender={(pageProps) => (
          <div>
            Hello world
            {pageProps.onRenderFooter()}
          </div>
        )} />
    );
  }
}

const container = document.getElementById(BehaviorGroupSchedulingConfig.containerId);
ReactDOM.render((
  <BehaviorGroupSchedulingLoader {...BehaviorGroupSchedulingConfig} />
), container);
