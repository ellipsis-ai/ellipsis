import * as React from 'react';
import autobind from '../lib/autobind';
import {PageRequiredProps} from "../shared_ui/page";

interface DashboardProps {
  csrfToken: string
}

type Props = DashboardProps & PageRequiredProps

class Dashboard extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  render() {
    return (
      <div>
        Hello world!
        {this.props.onRenderFooter()}
      </div>
    );
  }
}

export default Dashboard;
