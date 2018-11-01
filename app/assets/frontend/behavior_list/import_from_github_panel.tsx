import * as React from 'react';
import autobind from '../lib/autobind';

interface Props {
  // string: string
  // callback: () => void
}

class ImportFromGithubPanel extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  render() {
    return (
      <div className="box-action">
        <div className="container phn">
          Tada!
        </div>
      </div>
    );
  }
}

export default ImportFromGithubPanel;
