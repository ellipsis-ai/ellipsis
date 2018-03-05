import {ButtonProps} from './button';

import * as React from 'react';
import Button from './button';
import autobind from '../lib/autobind';

type Props = ButtonProps & {
  selected: boolean
};

class SidebarButton extends React.Component<Props> {
    constructor(props: Props) {
      super(props);
      autobind(this);
    }

    render() {
      return (
        <Button
          className={`button-block width-full phxl mobile-phl pvxs mvxs ${
            this.props.selected ? "bg-blue type-white" : "type-link"
          } ${this.props.className || ""}`}
          disabled={this.props.disabled}
          onClick={this.props.onClick}
          title={this.props.title}
        >{this.props.children}</Button>
      );
    }
}

export default SidebarButton;

