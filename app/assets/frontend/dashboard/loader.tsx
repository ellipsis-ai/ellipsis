import * as React from 'react';
import * as ReactDOM from 'react-dom';
import autobind from '../lib/autobind';

interface Props {
  // string: string
  // callback: () => void
}

class DashboardLoader extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  render() {
    return (
      <div>
        Hello world!
      </div>
    );
  }
}

const container = document.getElementById("dashboardContainer");
if (container) {
  ReactDOM.render((
    <DashboardLoader />
  ), container);
}
