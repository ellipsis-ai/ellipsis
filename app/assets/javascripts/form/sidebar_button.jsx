// @flow
import type {ButtonProps} from './button';

define(function(require) {
  const React = require('react'),
    Button = require('./button'),
    autobind = require('../lib/autobind');

  type Props = ButtonProps & {
    selected: boolean
  };

  class SidebarButton extends React.Component<Props> {
    props: Props;

    constructor(props): void {
      super(props);
      autobind(this);
    }

    render(): React.Node {
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

  return SidebarButton;
});
