import 'core-js';
import 'whatwg-fetch';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import Page from '../../shared_ui/page';
import autobind from "../../lib/autobind";
import SchedulingDataLayer, {SchedulingConfigInterface} from "../../scheduling/data_layer";
import BehaviorGroupConfigPage from "../behavior_group_config_page";

declare var BehaviorGroupSchedulingConfig: SchedulingConfigInterface & {
  groupId: string
};

interface State {
  sidebarWidth: number
}

class BehaviorGroupSchedulingLoader extends React.Component<SchedulingConfigInterface, State> {
  constructor(props: SchedulingConfigInterface) {
    super(props);
    autobind(this);
    this.state = {
      sidebarWidth: 0
    };
  }

  setSidebarWidth(newWidth: number): void {
    this.setState({
      sidebarWidth: newWidth
    });
  }

  render() {
    return (
      <Page csrfToken={this.props.csrfToken}
        onRender={(pageProps) => (
          <BehaviorGroupConfigPage activePage={"scheduling"} groupId={BehaviorGroupSchedulingConfig.groupId} onSidebarWidthChange={this.setSidebarWidth}>
            <SchedulingDataLayer {...BehaviorGroupSchedulingConfig} pageProps={pageProps} sidebarWidth={this.state.sidebarWidth} />
          </BehaviorGroupConfigPage>
        )} />
    );
  }
}

const container = document.getElementById(BehaviorGroupSchedulingConfig.containerId);
ReactDOM.render((
  <BehaviorGroupSchedulingLoader {...BehaviorGroupSchedulingConfig} />
), container);
