import * as React from 'react';
import autobind from '../lib/autobind';

interface Props {
  children: any
}

class DropdownContainer extends React.Component<Props> {
  container: Option<HTMLDivElement>;

  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  componentDidMount() {
    if (this.container) {
      this.container.addEventListener('click', (event) => {
        // TODO: Make this kosher
        event.ELLIPSIS_DROPDOWN = true;
      });
    }
  }

  render() {
    return (
      <div ref={(el) => this.container = el}>
        {this.props.children}
      </div>
    );
  }
}

export default DropdownContainer;
