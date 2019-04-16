import * as React from 'react';
import autobind from '../lib/autobind';

interface Props {
  // string: string
  // callback: () => void
}

class Dashboard extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  render() {
    return (
      <div>
      </div>
    );
  }
}

export default Dashboard;
